package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.RegistroSalud;
import com.agendapersonal.infrastructure.repo.SaludDAO;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Salud: lo de hoy en tarjetas (agua, sueño, ejercicio, peso, ánimo), registro rápido e historial. */
public class SaludView {
    /** Tipo guardado en la BD → etiqueta, unidad, ícono, color y meta diaria (0 = sin meta). */
    private record Tipo(String clave, String nombre, String unidad, String icono, String tono, double meta, String ejemplo) {}
    private static final List<Tipo> TIPOS = List.of(
        new Tipo("AGUA", "Agua", "vasos", "water", "blue", 8, "2"),
        new Tipo("SUENO", "Sueño", "h", "moon", "violet", 8, "7,5"),
        new Tipo("EJERCICIO", "Ejercicio", "min", "activity", "green", 30, "30"),
        new Tipo("PESO", "Peso", "kg", "scale", "teal", 0, "68,4"),
        new Tipo("ANIMO", "Ánimo", "/10", "mood", "amber", 0, "7"));

    private final SaludDAO dao = new SaludDAO();
    private final VBox view;
    private final TilePane tarjetas = new TilePane(16, 16);
    private final VBox historial = new VBox(0);
    private final Ui.Segmented rango = new Ui.Segmented("Esta semana", "Este mes", "Todo");
    private final ToggleGroup grupoTipo = new ToggleGroup();
    private final TextField valor = new TextField();
    private final Label unidad = Ui.label("", "input-suffix");
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TextField notas = new TextField();
    private Tipo tipo = TIPOS.getFirst();
    private ScrollPane scroll;

    public SaludView() {
        VBox page = Ui.page();
        page.getChildren().add(Ui.pageHeader("health", "Salud",
            "Un registro rápido al día: agua, sueño, ejercicio, peso y ánimo."));
        tarjetas.setPrefColumns(5);
        tarjetas.setPrefTileWidth(200);
        tarjetas.setTileAlignment(Pos.TOP_LEFT);
        rango.selectedProperty().addListener((o, a, b) -> fill());
        VBox hist = new VBox(0, historial);
        hist.getStyleClass().addAll("card", "card-flush");
        page.getChildren().addAll(tarjetas, formulario(), hist);
        view = page;
        fill();
    }

    private Node formulario() {
        FlowPane chips = new FlowPane(8, 8);
        for (Tipo t : TIPOS) {
            ToggleButton b = new ToggleButton(t.nombre(), IconFactory.icon(t.icono(), 16));
            b.getStyleClass().addAll("choice-chip", "choice-" + t.tono());
            b.setToggleGroup(grupoTipo);
            b.setUserData(t);
            b.setOnAction(e -> {
                if (!b.isSelected()) { b.setSelected(true); return; }
                tipo = t;
                actualizarUnidad();
                Anim.pop(b);
            });
            chips.getChildren().add(b);
        }
        ((ToggleButton) chips.getChildren().getFirst()).setSelected(true);

        valor.getStyleClass().add("bare-field");
        HBox valorBox = new HBox(8, valor, unidad);
        valorBox.getStyleClass().add("input-with-icon");
        valorBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valor, Priority.ALWAYS);
        notas.setPromptText("Ej. Después del desayuno");
        Ui.fechaCorta(fecha);
        unidad.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button add = Ui.primary("add", "Registrar");
        add.setOnAction(e -> registrar(valorBox));
        valor.setOnAction(e -> registrar(valorBox));
        notas.setOnAction(e -> registrar(valorBox));
        actualizarUnidad();

        VBox fValor = Ui.field("Cantidad", valorBox);
        VBox fFecha = Ui.field("Fecha", fecha);
        VBox fNotas = Ui.field("Notas (opcional)", notas);
        fValor.setPrefWidth(170); fFecha.setPrefWidth(180); fNotas.setPrefWidth(320);
        HBox fila = new HBox(12, fValor, fFecha, fNotas, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox card = Ui.card();
        card.setSpacing(14);
        card.getChildren().addAll(Ui.h3("Nuevo registro"), chips, fila);
        return card;
    }

    private void actualizarUnidad() {
        unidad.setText(tipo.unidad());
        valor.setPromptText("Ej. " + tipo.ejemplo());
    }

    private void registrar(Node valorBox) {
        double v;
        try { v = Ui.parseNumber(valor.getText()); }
        catch (Exception x) { Anim.shake(valorBox); Notifier.warning("Valor inválido", "Escribe un número, por ejemplo " + tipo.ejemplo() + "."); return; }
        if (v < 0 || ("ANIMO".equals(tipo.clave()) && v > 10)) {
            Anim.shake(valorBox);
            Notifier.warning("Valor fuera de rango", "ANIMO".equals(tipo.clave()) ? "El ánimo va de 0 a 10." : "El valor no puede ser negativo.");
            return;
        }
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        if (dao.insertar(new RegistroSalud(0, tipo.clave(), v, tipo.unidad(), fecha.getValue().toString(), notas.getText().trim()))) {
            Notifier.success(tipo.nombre() + " registrado", num(v) + " " + tipo.unidad() + (fecha.getValue().equals(LocalDate.now()) ? " hoy" : ""));
            valor.clear();
            notas.clear();
            fill();
        } else Notifier.error("No se pudo guardar el registro", null);
    }

    private void fill() {
        List<RegistroSalud> todos = dao.listar(null, 500);
        LocalDate hoy = LocalDate.now();
        String h = hoy.toString();

        // ---- tarjetas de hoy
        tarjetas.getChildren().clear();
        for (Tipo t : TIPOS) {
            List<RegistroSalud> deTipo = todos.stream().filter(r -> t.clave().equals(r.getTipo())).toList();
            List<RegistroSalud> deHoy = deTipo.stream().filter(r -> h.equals(r.getFecha())).toList();
            String valorTxt, nota;
            double progreso = -1;
            switch (t.clave()) {
                case "AGUA", "EJERCICIO" -> {
                    double suma = deHoy.stream().mapToDouble(RegistroSalud::getValor).sum();
                    valorTxt = num(suma) + " " + t.unidad();
                    nota = "Meta: " + num(t.meta()) + " " + t.unidad();
                    progreso = suma / t.meta();
                }
                case "SUENO" -> {
                    if (deHoy.isEmpty()) { valorTxt = "—"; nota = "Registra cómo dormiste"; }
                    else {
                        double v = deHoy.getFirst().getValor();
                        valorTxt = horas(v);
                        nota = v >= t.meta() ? "¡Meta de 8 h cumplida!" : "Meta: 8 h";
                        progreso = v / t.meta();
                    }
                }
                case "PESO" -> {
                    if (deTipo.isEmpty()) { valorTxt = "—"; nota = "Aún sin registros"; }
                    else {
                        RegistroSalud ult = deTipo.getFirst();
                        valorTxt = num(ult.getValor()) + " kg";
                        nota = cambioPeso(deTipo, ult);
                    }
                }
                default -> {
                    if (deHoy.isEmpty()) { valorTxt = "—"; nota = "¿Cómo te sientes hoy?"; }
                    else {
                        double v = deHoy.getFirst().getValor();
                        valorTxt = v >= 8 ? "Muy bien" : v >= 6 ? "Bien" : v >= 4 ? "Regular" : "Bajo";
                        nota = num(v) + " de 10";
                        progreso = v / 10.0;
                    }
                }
            }
            tarjetas.getChildren().add(tarjeta(t, valorTxt, nota, progreso));
        }

        // ---- historial
        LocalDate desde = switch (rango.selected()) { case 0 -> hoy.minusDays(6); case 1 -> hoy.withDayOfMonth(1); default -> LocalDate.MIN; };
        List<RegistroSalud> lista = todos.stream().filter(r -> {
            try { return !LocalDate.parse(r.getFecha()).isBefore(desde); } catch (Exception e) { return true; }
        }).toList();
        historial.getChildren().clear();
        HBox head = new HBox(8, Ui.h3("Historial"), Ui.spacer(), rango.node());
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(16, 22, 10, 22));
        historial.getChildren().add(head);
        if (lista.isEmpty()) {
            historial.getChildren().add(Ui.empty("health", "Sin registros en este periodo", "Usa el formulario de arriba para anotar tu día."));
            return;
        }
        GridPane th = gridFila();
        th.add(Ui.overline("Fecha"), 0, 0);
        th.add(Ui.overline("Tipo"), 1, 0);
        th.add(Ui.overline("Valor"), 2, 0);
        th.add(Ui.overline("Notas"), 3, 0);
        th.setPadding(new Insets(4, 22, 8, 22));
        historial.getChildren().add(th);
        List<Node> filas = new ArrayList<>();
        for (RegistroSalud r : lista) { Node f = fila(r); historial.getChildren().add(f); filas.add(f); }
        Anim.stagger(filas);
    }

    private static String cambioPeso(List<RegistroSalud> pesos, RegistroSalud ult) {
        try {
            LocalDate fUlt = LocalDate.parse(ult.getFecha());
            for (RegistroSalud r : pesos) {
                LocalDate f = LocalDate.parse(r.getFecha());
                if (!f.isAfter(fUlt.minusDays(6)) && r != ult) {
                    double d = ult.getValor() - r.getValor();
                    if (Math.abs(d) < 0.05) return "Estable esta semana";
                    return (d > 0 ? "+" : "−") + num(Math.abs(d)) + " kg en una semana";
                }
            }
        } catch (Exception ignored) {}
        return "Último registro: " + Ui.friendlyDate(LocalDate.parse(ult.getFecha())).toLowerCase(Locale.ROOT);
    }

    private VBox tarjeta(Tipo t, String valorTxt, String nota, double progreso) {
        HBox top = new HBox(10, Ui.iconChip(t.icono(), t.tono(), 34, 18), Ui.label(t.nombre(), "stat-label"));
        top.setAlignment(Pos.CENTER_LEFT);
        Label v = Ui.label(valorTxt, "stat-value", "stat-value-sm");
        VBox b = new VBox(10, top, v);
        if (progreso >= 0) b.getChildren().add(Ui.bar(progreso, t.tono(), 6));
        b.getChildren().add(Ui.label(nota, "stat-note"));
        b.getStyleClass().addAll("card", "stat-card");
        b.setPadding(new Insets(18, 20, 18, 20));
        b.setPrefWidth(200);
        b.setMinHeight(150);
        return b;
    }

    private GridPane gridFila() {
        GridPane g = new GridPane();
        g.setHgap(16);
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints(120);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setHgrow(Priority.ALWAYS);
        ColumnConstraints c4 = new ColumnConstraints(40);
        g.getColumnConstraints().addAll(c0, c1, c2, c3, c4);
        g.setAlignment(Pos.CENTER_LEFT);
        return g;
    }

    private Node fila(RegistroSalud r) {
        Tipo t = TIPOS.stream().filter(x -> x.clave().equals(r.getTipo())).findFirst().orElse(TIPOS.getLast());
        String cuando;
        try { cuando = Ui.friendlyDate(LocalDate.parse(r.getFecha())); } catch (Exception e) { cuando = r.getFecha(); }
        Label pill = Ui.pill(t.nombre(), t.tono());
        pill.setGraphic(IconFactory.icon(t.icono(), 13, "tint-" + t.tono()));
        pill.setGraphicTextGap(6);
        String unidadTxt = r.getUnidad() != null ? r.getUnidad() : t.unidad();
        Label val = Ui.label("SUENO".equals(t.clave()) ? horas(r.getValor()) : num(r.getValor()) + " " + unidadTxt, "row-title");
        Label nota = Ui.label(r.getNotas() == null || r.getNotas().isBlank() ? "—" : r.getNotas(), "row-meta");
        Button mas = Ui.iconButton("more", "Más opciones");
        MenuItem borrar = new MenuItem("Eliminar registro");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (Ui.confirm(view, "¿Eliminar este registro?", t.nombre() + " · " + val.getText(), "Eliminar") && dao.eliminar(r.getId())) {
                fill();
                Notifier.deleted("Registro eliminado", t.nombre() + " · " + val.getText());
            }
        });
        ContextMenu cm = new ContextMenu(borrar);
        mas.setOnAction(e -> cm.show(mas, javafx.geometry.Side.BOTTOM, 0, 4));
        GridPane g = gridFila();
        g.add(Ui.label(cuando, "row-meta-strong"), 0, 0);
        g.add(pill, 1, 0);
        g.add(val, 2, 0);
        g.add(nota, 3, 0);
        g.add(mas, 4, 0);
        g.getStyleClass().addAll("list-row", "with-divider");
        g.setPadding(new Insets(10, 22, 10, 22));
        return g;
    }

    private static String num(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf(Math.round(v));
        return String.format(Ui.ES, "%.1f", v);
    }

    private static String horas(double v) {
        int h = (int) v;
        int m = (int) Math.round((v - h) * 60);
        if (m == 60) { h++; m = 0; }
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    public Node getView() {
        if (scroll == null) scroll = Ui.scroll(view);
        return scroll;
    }
}
