package com.agendapersonal.util;

import com.agendapersonal.infrastructure.db.ConfigDAO;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Datos del dueño de la agenda (solo su nombre; Compás es local y de un solo usuario, no hay contraseña). */
public final class Perfil {
    public static final String CFG_NOMBRE = "usuario.nombre";
    public static final String CFG_BIENVENIDA = "usuario.bienvenida";   // "1" cuando ya se mostró la bienvenida
    public static final String CFG_ROL = "usuario.rol";
    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String DOCENTE = "DOCENTE";
    private static final int MAX = 40;
    private static final IntegerProperty REVISION = new SimpleIntegerProperty(0);
    private static final ConfigDAO CFG = new ConfigDAO();
    private Perfil() {}

    /** Cambia cuando se edita el nombre (para que el saludo del inicio se actualice solo). */
    public static IntegerProperty revisionProperty() { return REVISION; }

    public static String nombre() { return CFG.get(CFG_NOMBRE, ""); }
    public static String rol() { return CFG.get(CFG_ROL, ESTUDIANTE); }
    public static boolean esDocente() { return DOCENTE.equals(rol()); }
    public static void guardarRol(String rol) {
        CFG.set(CFG_ROL, DOCENTE.equals(rol) ? DOCENTE : ESTUDIANTE);
        REVISION.set(REVISION.get() + 1);
    }
    public static boolean bienvenidaVista() { return "1".equals(CFG.get(CFG_BIENVENIDA, "0")); }

    public static void guardarNombre(String texto) {
        CFG.set(CFG_NOMBRE, limpiar(texto));
        CFG.set(CFG_BIENVENIDA, "1");
        REVISION.set(REVISION.get() + 1);
    }

    public static void omitirBienvenida() { CFG.set(CFG_BIENVENIDA, "1"); }

    // ---- lógica pura (con pruebas) ----

    /** Recorta, une espacios repetidos y limita el largo. */
    public static String limpiar(String texto) {
        if (texto == null) return "";
        String t = texto.trim().replaceAll("\\s+", " ");
        return t.length() > MAX ? t.substring(0, MAX).trim() : t;
    }

    /** "jason selmouni" → "Jason" (solo la primera palabra, con inicial mayúscula). */
    public static String primerNombre(String nombre) {
        String t = limpiar(nombre);
        if (t.isEmpty()) return "";
        String p = t.split(" ")[0];
        return Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    /** "Buenas noches, Jason" (o solo "Buenas noches" si aún no hay nombre). */
    public static String saludo(int hora, String nombre) {
        String base = hora < 12 ? "Buenos días" : hora < 20 ? "Buenas tardes" : "Buenas noches";
        String n = primerNombre(nombre);
        return n.isEmpty() ? base : base + ", " + n;
    }
}
