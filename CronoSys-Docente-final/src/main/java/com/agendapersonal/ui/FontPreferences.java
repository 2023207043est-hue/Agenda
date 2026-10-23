package com.agendapersonal.ui;

import com.agendapersonal.settings.SettingsStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tipografía de toda la interfaz: familia y tamaño del texto, guardados en la configuración.
 * Incluye fuentes propias (carpeta {@code /fonts}) para que la app se vea igual en cualquier PC.
 */
public final class FontPreferences {
    /** Fuentes que vienen dentro de Compás (archivo base en /fonts, sin "-Regular.ttf"). */
    private static final String[][] BUNDLED = {
        {"Plus Jakarta Sans", "PlusJakartaSans"},
        {"Inter", "Inter"},
        {"IBM Plex Sans", "IBMPlexSans"},
        {"Source Sans 3", "SourceSans3"},
        {"Roboto", "Roboto"},
        {"JetBrains Mono", "JetBrainsMono"},
    };
    /** Fuentes del sistema (Windows) que también se ofrecen. */
    private static final List<String> SYSTEM = List.of(
        "Segoe UI", "Calibri", "Arial", "Verdana", "Tahoma", "Georgia",
        "Corbel", "Constantia", "Comic Sans MS", "Consolas", "Courier New"
    );

    /** Tamaños de texto: etiqueta visible → tamaño base en px. */
    public enum Size {
        PEQUENO("Pequeño", 12.5), NORMAL("Normal", 14), GRANDE("Grande", 15.5), MUY_GRANDE("Muy grande", 17);
        private final String label; private final double px;
        Size(String label, double px) { this.label = label; this.px = px; }
        public double px() { return px; }
        @Override public String toString() { return label; }
        public static Size from(String v) {
            try { return valueOf(v); } catch (Exception e) { return NORMAL; }
        }
    }

    private static final SettingsStore STORE = SettingsStore.get();
    private static boolean loaded;
    private static final StringProperty FAMILY = new SimpleStringProperty();
    private static final DoubleProperty BASE = new SimpleDoubleProperty();

    static {
        cargarFuentesIncluidas();
        FAMILY.set(normalize(STORE.str("appearance.font")));
        BASE.set(14.0 * STORE.integer("appearance.fontScale", 85, 125) / 100.0);
    }

    private FontPreferences() {}

    /** Registra en JavaFX las fuentes incluidas (se llama solo una vez). */
    public static synchronized void cargarFuentesIncluidas() {
        if (loaded) return;
        loaded = true;
        for (String[] f : BUNDLED) {
            for (String peso : new String[]{"Regular", "Bold"}) {
                try (InputStream in = FontPreferences.class.getResourceAsStream("/fonts/" + f[1] + "-" + peso + ".ttf")) {
                    if (in != null) Font.loadFont(in, 13);
                } catch (Exception ignored) { /* si falla, se usa una fuente del sistema */ }
            }
        }
    }

    /** Opciones para el selector: primero las incluidas, luego las del sistema que estén instaladas. */
    public static List<String> options() {
        Set<String> out = new LinkedHashSet<>();
        Set<String> instaladas = new java.util.HashSet<>(Font.getFamilies());
        for (String[] f : BUNDLED) if (instaladas.contains(f[0])) out.add(f[0]);
        for (String s : SYSTEM) if (instaladas.contains(s)) out.add(s);
        if (out.isEmpty()) out.add(Font.getDefault().getFamily());
        return new ArrayList<>(out);
    }

    public static boolean esIncluida(String family) {
        for (String[] f : BUNDLED) if (f[0].equals(family)) return true;
        return false;
    }

    public static String family() { return FAMILY.get(); }
    public static StringProperty familyProperty() { return FAMILY; }
    public static double baseSize() { return BASE.get(); }
    public static DoubleProperty baseSizeProperty() { return BASE; }
    public static Size size() {
        for (Size s : Size.values()) if (Math.abs(s.px() - BASE.get()) < 0.1) return s;
        return Size.NORMAL;
    }

    public static void setFamily(String selected) {
        FAMILY.set(normalize(selected));
        STORE.set("appearance.font", FAMILY.get());
        ThemeService.get().refresh();
    }

    public static void setSize(Size s) {
        if (s == null) return;
        setScale((int) Math.round(s.px() / 14.0 * 100));
    }

    /** Escala de texto en porcentaje (85 a 125). Pequeño/Normal/Grande/Muy grande son atajos de este valor. */
    public static void setScale(int percent) {
        int p = Math.max(85, Math.min(125, percent));
        BASE.set(14.0 * p / 100.0);
        STORE.set("appearance.fontScale", p);
        ThemeService.get().refresh();
    }

    public static int scalePercent() { return (int) Math.round(BASE.get() / 14.0 * 100); }

    /** Recarga familia y escala desde los ajustes (al cambiar de perfil o importar). */
    public static void reload() {
        FAMILY.set(normalize(STORE.str("appearance.font")));
        BASE.set(14.0 * STORE.integer("appearance.fontScale", 85, 125) / 100.0);
    }

    // Compatibilidad con el código anterior.
    public static void setFamily(String selected, Parent ignored) { setFamily(selected); }
    public static void apply(Parent ignored) { ThemeService.get().refresh(); }

    /**
     * Fuente fija (p. ej. un título de 22 px en negrita) que sigue a la familia y al tamaño elegidos.
     * {@code size} está pensado para el tamaño "Normal" (14 px) y se escala con la preferencia.
     */
    public static void bind(Labeled node, FontWeight weight, double size) {
        node.fontProperty().bind(Bindings.createObjectBinding(
            () -> Font.font(FAMILY.get(), weight, size * BASE.get() / 14.0), FAMILY, BASE));
    }

    private static String normalize(String value) {
        if (value == null) return "Plus Jakarta Sans";
        for (String o : options()) if (o.equalsIgnoreCase(value.trim())) return o;
        return options().getFirst();
    }
}
