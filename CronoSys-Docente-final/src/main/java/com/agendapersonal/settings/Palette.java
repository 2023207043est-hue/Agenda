package com.agendapersonal.settings;

import java.util.Map;

/**
 * Deriva los tokens de color de la interfaz a partir de los cuatro colores base del tema
 * (fondo, tarjeta, texto y acento). Los temas "claro" y "oscuro" originales no pasan por aquí.
 */
public final class Palette {
    private Palette() {}

    /** Resultado de resolver los ajustes de apariencia. */
    public record Resolved(boolean dark, String bg, String card, String text, String accent, boolean legacy) {}

    /** Resuelve el tema activo: preset + colores personalizados + modo (claro/oscuro/automático). */
    public static Resolved resolve(SettingsStore s, java.time.LocalTime now) {
        ThemePreset t = ThemePreset.byId(s.str("appearance.themeId"));
        String mode = s.choice("appearance.mode", "claro", "oscuro", "automatico");
        boolean auto = s.bool("appearance.autoThemeByTime") || "automatico".equals(mode);
        if (t.isLegacy()) {
            boolean dark;
            if (auto) dark = isNight(s, now);
            else dark = "oscuro".equals(mode) ? true : "claro".equals(mode) ? false : t.dark();
            // El selector de modo manda sobre los dos temas originales.
            t = ThemePreset.byId(dark ? "oscuro" : "claro");
        }
        String bg = custom(s, "bg", t.bg()), card = custom(s, "card", t.card()), text = custom(s, "text", t.text());
        String accent = custom(s, "accent", null);
        boolean legacy = t.isLegacy() && bg.equals(t.bg()) && card.equals(t.card()) && text.equals(t.text());
        if (accent == null) accent = t.isLegacy() ? null : t.accent();
        boolean dark = t.isLegacy() ? t.dark() : Colors.isDark(bg);
        return new Resolved(dark, bg, card, text, accent, legacy);
    }

    private static String custom(SettingsStore s, String key, String def) {
        String v = Colors.normalize(s.str("appearance.colors." + key));
        return v != null ? v : def;
    }

    private static boolean isNight(SettingsStore s, java.time.LocalTime now) {
        int from = SettingsStore.minutes(s.str("appearance.autoDarkFrom")), to = SettingsStore.minutes(s.str("appearance.autoDarkTo"));
        int m = now.getHour() * 60 + now.getMinute();
        return from <= to ? (m >= from && m < to) : (m >= from || m < to);
    }

    /** Sobrescribe en {@code p} los tokens de superficie, texto y acento a partir de la base. */
    public static void apply(Map<String, String> p, Resolved r) {
        String bg = r.bg(), card = r.card(), text = r.text();
        boolean d = r.dark();
        String toward = d ? "#FFFFFF" : "#000000";
        p.put("-bg", bg);
        p.put("-surface", card);
        p.put("-surface2", Colors.mix(card, toward, d ? 0.05 : 0.04));
        p.put("-surface3", Colors.mix(card, toward, d ? 0.10 : 0.08));
        p.put("-hover", Colors.mix(card, toward, d ? 0.07 : 0.05));
        p.put("-bord", Colors.mix(bg, toward, d ? 0.12 : 0.10));
        p.put("-bord-strong", Colors.mix(bg, toward, d ? 0.22 : 0.18));
        p.put("-text", text);
        p.put("-text2", Colors.mix(text, bg, 0.22));
        p.put("-muted", Colors.mix(text, bg, 0.40));
        p.put("-icon", Colors.mix(text, bg, 0.32));
        p.put("-sidebar", Colors.mix(bg, d ? "#000000" : "#FFFFFF", d ? 0.18 : 0.0));
        p.put("-input", d ? Colors.mix(card, "#FFFFFF", 0.05) : card);
        p.put("-inverse", text);
        p.put("-on-inverse", bg);
        p.put("-on-inverse-muted", Colors.mix(bg, text, 0.30));
        p.put("-shadow", d ? "rgba(0,0,0,0.35)" : "rgba(28,31,38,0.08)");
    }

    /** Aplica el acento (hex) y calcula el texto sobre él y su fondo suave. */
    public static void applyAccent(Map<String, String> p, String accent, String bg, boolean dark) {
        p.put("-accent", accent);
        p.put("-accent-soft", Colors.mix(bg, accent, dark ? 0.22 : 0.14));
        p.put("-on-accent", Colors.onColor(accent));
    }

    /** Avisos discretos de poco contraste (texto sobre fondo y texto sobre acento). Lista vacía = todo bien. */
    public static java.util.List<String> contrastWarnings(String bg, String card, String text, String accent) {
        java.util.List<String> w = new java.util.ArrayList<>();
        if (Colors.contrast(text, bg) < 4.5) w.add("El texto tiene poco contraste con el fondo.");
        if (Colors.contrast(text, card) < 4.5) w.add("El texto tiene poco contraste con las tarjetas.");
        if (Colors.contrast(Colors.onColor(accent), accent) < 4.5) w.add("El texto sobre el acento se lee con dificultad.");
        if (Colors.contrast(accent, bg) < 2.0) w.add("El acento se distingue poco del fondo.");
        return w;
    }
}
