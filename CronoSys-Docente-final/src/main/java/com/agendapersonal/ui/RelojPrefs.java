package com.agendapersonal.ui;

import com.agendapersonal.infrastructure.db.ConfigDAO;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Preferencias del reloj grande del inicio (formato 12/24 h, segundos, tamaño, visible). */
public final class RelojPrefs {
    public enum Tamano {
        PEQUENO("Pequeño", 40), MEDIANO("Mediano", 64), GRANDE("Grande", 88);
        private final String etiqueta; private final int px;
        Tamano(String e, int px) { this.etiqueta = e; this.px = px; }
        public String etiqueta() { return etiqueta; }
        public int px() { return px; }
        public static Tamano desde(String v) { try { return valueOf(v); } catch (Exception e) { return MEDIANO; } }
    }

    private static final String K_24H = "reloj.h24", K_SEG = "reloj.segundos", K_TAM = "reloj.tamano", K_VISIBLE = "reloj.visible";
    private static final ConfigDAO CFG = new ConfigDAO();
    private static final IntegerProperty REVISION = new SimpleIntegerProperty(0);
    private RelojPrefs() {}

    /** Cambia cada vez que se modifica una preferencia (el inicio se redibuja solo). */
    public static IntegerProperty revisionProperty() { return REVISION; }

    public static boolean formato24() { return !"0".equals(CFG.get(K_24H, "1")); }
    public static boolean segundos() { return "1".equals(CFG.get(K_SEG, "0")); }
    public static boolean visible() { return !"0".equals(CFG.get(K_VISIBLE, "1")); }
    public static Tamano tamano() { return Tamano.desde(CFG.get(K_TAM, "MEDIANO")); }

    public static void setFormato24(boolean v) { guardar(K_24H, v ? "1" : "0"); }
    public static void setSegundos(boolean v) { guardar(K_SEG, v ? "1" : "0"); }
    public static void setVisible(boolean v) { guardar(K_VISIBLE, v ? "1" : "0"); }
    public static void setTamano(Tamano t) { guardar(K_TAM, t.name()); }

    private static void guardar(String k, String v) { CFG.set(k, v); REVISION.set(REVISION.get() + 1); }

    /** Patrón de hora: "HH:mm", "HH:mm:ss", "h:mm a" o "h:mm:ss a". */
    public static String patron(boolean h24, boolean conSegundos) {
        String s = conSegundos ? ":ss" : "";
        return h24 ? "HH:mm" + s : "h:mm" + s + " a";
    }

    public static String formatear(LocalTime t, boolean h24, boolean conSegundos) {
        return t.format(DateTimeFormatter.ofPattern(patron(h24, conSegundos), com.agendapersonal.util.Ui.ES));
    }

    public static String formatearAhora() { return formatear(LocalTime.now(), formato24(), segundos()); }
}
