package com.agendapersonal.service;

import com.agendapersonal.domain.model.ClaseDocente;
import com.agendapersonal.domain.model.EstudianteClase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Importa y exporta libros XLSX sencillos para la lista de estudiantes y sus calificaciones. */
public final class ExcelClaseService {
    public record ResultadoImportacion(List<EstudianteClase> estudiantes, int filasOmitidas) {}

    private static final String SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String OFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private ExcelClaseService() {}

    public static void exportar(Path destino, ClaseDocente clase, List<EstudianteClase> estudiantes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(destino), StandardCharsets.UTF_8)) {
            entrada(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """);
            entrada(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            entrada(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Estudiantes" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """);
            entrada(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """);
            StringBuilder sheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
                  <sheetData>
                """);
            List<String> encabezados = List.of("Estudiante", "Correo", "Calificación", "Observaciones");
            sheet.append("<row r=\"1\">");
            for (int c = 0; c < encabezados.size(); c++) celdaTexto(sheet, c, 1, encabezados.get(c));
            sheet.append("</row>");
            int fila = 2;
            for (EstudianteClase estudiante : estudiantes) {
                sheet.append("<row r=\"").append(fila).append("\">");
                celdaTexto(sheet, 0, fila, estudiante.nombre());
                celdaTexto(sheet, 1, fila, estudiante.correo());
                if (estudiante.calificacion() != null && Double.isFinite(estudiante.calificacion()))
                    sheet.append("<c r=\"").append(referencia(2, fila)).append("\"><v>")
                        .append(Double.toString(estudiante.calificacion())).append("</v></c>");
                else celdaTexto(sheet, 2, fila, "");
                celdaTexto(sheet, 3, fila, estudiante.observaciones());
                sheet.append("</row>");
                fila++;
            }
            sheet.append("</sheetData>");
            if (fila > 2) sheet.append("<autoFilter ref=\"A1:D").append(fila - 1).append("\"/>");
            sheet.append("</worksheet>");
            entrada(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    public static ResultadoImportacion importar(Path origen, int claseId) throws IOException {
        try (ZipFile zip = new ZipFile(origen.toFile(), StandardCharsets.UTF_8)) {
            Document workbook = documento(zip, "xl/workbook.xml");
            Element sheet = (Element) workbook.getElementsByTagNameNS(SPREADSHEET_NS, "sheet").item(0);
            if (sheet == null) throw new IOException("El libro no contiene hojas de cálculo.");
            String relationshipId = sheet.getAttributeNS(OFFICE_REL_NS, "id");
            Document rels = documento(zip, "xl/_rels/workbook.xml.rels");
            NodeList relationships = rels.getElementsByTagNameNS(PACKAGE_REL_NS, "Relationship");
            String target = null;
            for (int i = 0; i < relationships.getLength(); i++) {
                Element rel = (Element) relationships.item(i);
                if (relationshipId.equals(rel.getAttribute("Id"))) { target = rel.getAttribute("Target"); break; }
            }
            if (target == null || target.isBlank()) throw new IOException("No se encontró la primera hoja del libro.");
            String sheetPath = resolverRutaHoja(target);
            List<String> shared = zip.getEntry("xl/sharedStrings.xml") == null
                ? List.of() : leerSharedStrings(documento(zip, "xl/sharedStrings.xml"));
            List<List<String>> filas = leerFilas(documento(zip, sheetPath), shared);
            return mapear(filas, claseId);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static ResultadoImportacion mapear(List<List<String>> filas, int claseId) {
        int cabecera = -1;
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            Map<String, Integer> posibles = new HashMap<>();
            for (int c = 0; c < fila.size(); c++) {
                String h = normalizar(fila.get(c));
                if (esNombre(h)) posibles.put("nombre", c);
                if (esCorreo(h)) posibles.put("correo", c);
                if (esNota(h)) posibles.put("calificacion", c);
                if (esObservacion(h)) posibles.put("observaciones", c);
            }
            if (posibles.containsKey("nombre")) { cabecera = i; columnas = posibles; break; }
        }
        if (cabecera < 0) throw new IllegalArgumentException("No encontré una columna de estudiante. Usa el encabezado «Estudiante» o «Nombre».");

        List<EstudianteClase> resultado = new ArrayList<>();
        int omitidas = 0;
        for (int r = cabecera + 1; r < filas.size(); r++) {
            List<String> fila = filas.get(r);
            String nombre = valor(fila, columnas.get("nombre")).trim();
            if (nombre.isBlank()) continue;
            String correo = valor(fila, columnas.get("correo")).trim();
            String notaTexto = valor(fila, columnas.get("calificacion")).trim();
            String observaciones = valor(fila, columnas.get("observaciones")).trim();
            Double nota = null;
            if (!notaTexto.isBlank()) {
                try { nota = Double.valueOf(notaTexto.replace(',', '.')); }
                catch (NumberFormatException e) { omitidas++; continue; }
                if (!Double.isFinite(nota)) { omitidas++; continue; }
            }
            resultado.add(new EstudianteClase(0, claseId, nombre, correo.isBlank() ? null : correo,
                nota, observaciones.isBlank() ? null : observaciones));
        }
        return new ResultadoImportacion(List.copyOf(resultado), omitidas);
    }

    private static List<List<String>> leerFilas(Document doc, List<String> shared) throws IOException {
        NodeList rows = doc.getElementsByTagNameNS(SPREADSHEET_NS, "row");
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            List<String> values = new ArrayList<>();
            NodeList children = row.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (!(node instanceof Element cell) || !"c".equals(cell.getLocalName())) continue;
                int col = columna(cell.getAttribute("r"));
                while (values.size() <= col) values.add("");
                values.set(col, leerCelda(cell, shared));
            }
            filas.add(values);
        }
        return filas;
    }

    private static String leerCelda(Element cell, List<String> shared) throws IOException {
        String tipo = cell.getAttribute("t");
        if ("inlineStr".equals(tipo)) return textoDescendiente(cell, "t");
        NodeList values = cell.getElementsByTagNameNS(SPREADSHEET_NS, "v");
        if (values.getLength() == 0) return "";
        String value = values.item(0).getTextContent();
        if ("s".equals(tipo)) {
            try { return shared.get(Integer.parseInt(value)); }
            catch (RuntimeException e) { throw new IOException("El libro contiene una referencia de texto compartido inválida.", e); }
        }
        return value;
    }

    private static List<String> leerSharedStrings(Document doc) {
        NodeList items = doc.getElementsByTagNameNS(SPREADSHEET_NS, "si");
        List<String> salida = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) salida.add(textoDescendiente((Element) items.item(i), "t"));
        return salida;
    }

    private static String textoDescendiente(Element raiz, String nombre) {
        NodeList textos = raiz.getElementsByTagNameNS(SPREADSHEET_NS, nombre);
        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < textos.getLength(); i++) salida.append(textos.item(i).getTextContent());
        return salida.toString();
    }

    private static Document documento(ZipFile zip, String ruta) throws IOException {
        ZipEntry entry = zip.getEntry(ruta);
        if (entry == null) throw new IOException("El archivo no parece un Excel .xlsx válido (falta " + ruta + ").");
        try (InputStream in = zip.getInputStream(entry)) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return f.newDocumentBuilder().parse(in);
        } catch (Exception e) { throw new IOException("No pude leer el contenido de Excel: " + e.getMessage(), e); }
    }

    private static String resolverRutaHoja(String target) {
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")) return normalized.substring(1);
        return Path.of("xl").resolve(normalized).normalize().toString().replace('\\', '/');
    }

    private static int columna(String referencia) {
        int n = 0;
        for (int i = 0; i < referencia.length(); i++) {
            char ch = referencia.charAt(i);
            if (!Character.isLetter(ch)) break;
            n = n * 26 + Character.toUpperCase(ch) - 'A' + 1;
        }
        return Math.max(0, n - 1);
    }

    private static String referencia(int columna, int fila) {
        StringBuilder letras = new StringBuilder();
        for (int n = columna + 1; n > 0; n = (n - 1) / 26) letras.insert(0, (char) ('A' + (n - 1) % 26));
        return letras + Integer.toString(fila);
    }

    private static void celdaTexto(StringBuilder sheet, int columna, int fila, String valor) {
        sheet.append("<c r=\"").append(referencia(columna, fila)).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
            .append(escapar(valor == null ? "" : valor)).append("</t></is></c>");
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void entrada(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String valor(List<String> fila, Integer indice) {
        return indice == null || indice < 0 || indice >= fila.size() ? "" : fila.get(indice);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean esNombre(String s) { return List.of("estudiante", "nombre", "alumno", "alumna").contains(s); }
    private static boolean esCorreo(String s) { return List.of("correo", "email", "e-mail", "mail").contains(s); }
    private static boolean esNota(String s) { return List.of("calificacion", "nota", "grade", "puntaje", "nota final").contains(s); }
    private static boolean esObservacion(String s) { return List.of("observaciones", "comentarios", "notes", "comentario").contains(s); }
}
