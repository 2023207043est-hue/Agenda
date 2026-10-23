package com.agendapersonal.settings;

import java.util.List;

/** Un tema predefinido es solo una combinación de colores base. */
public record ThemePreset(String id, String name, boolean dark, String bg, String card, String text, String accent) {

    /** "claro" y "oscuro" son los temas originales: conservan su paleta exacta (ver ThemeService). */
    public boolean isLegacy() { return id.equals("claro") || id.equals("oscuro"); }

    public static final List<ThemePreset> ALL = List.of(
        new ThemePreset("claro", "Claro", false, "#F5F4F0", "#FFFFFF", "#1C1F26", "#C64B1C"),
        new ThemePreset("oscuro", "Oscuro", true, "#121419", "#1B1E25", "#ECEDF0", "#F08A5D"),
        new ThemePreset("indigo", "Oscuro índigo", true, "#141824", "#1B2030", "#E6E9F2", "#F2874F"),
        new ThemePreset("oceano", "Océano", true, "#10222B", "#16303B", "#D4E6EA", "#5FB3B3"),
        new ThemePreset("atardecer", "Atardecer", true, "#241C17", "#2F251E", "#EADCC8", "#E0955A"),
        new ThemePreset("salvia", "Salvia", false, "#EEF0E8", "#F7F8F3", "#3B4636", "#6F9159"),
        new ThemePreset("lavanda", "Lavanda", false, "#F1EEF8", "#FAF8FF", "#463F63", "#8B74D1"),
        new ThemePreset("kawaii", "Kawaii", true, "#2B2240", "#34294F", "#F6EEFC", "#F48FB8"),
        new ThemePreset("neon", "Neón", true, "#0A0614", "#120A2A", "#EAFFFF", "#00F0FF"),
        new ThemePreset("grafito", "Grafito", true, "#0F1319", "#1E232B", "#D5DBE4", "#5B8FC7"),
        new ThemePreset("tactico", "Táctico", true, "#161A12", "#212819", "#D9DCCB", "#A6B66A"),
        new ThemePreset("garage", "Garage", true, "#1A1A1C", "#242427", "#E0DDD8", "#E8833A"),
        new ThemePreset("gamer", "Gamer nocturno", true, "#16161E", "#1F2335", "#C0CAF5", "#7AA2F7"),
        new ThemePreset("blush", "Rosa blush", false, "#F8E8E6", "#FFFAF8", "#5C3F45", "#CF6A82"),
        new ThemePreset("matcha", "Salvia matcha", false, "#E6ECD8", "#F7F8F3", "#3B4636", "#6F9159"),
        new ThemePreset("oceano-calma", "Océano calma", true, "#0D1C24", "#16303B", "#D4E6EA", "#5FB3B3"),
        new ThemePreset("lectura", "Atardecer lectura", true, "#241C17", "#2F251E", "#EADCC8", "#E0955A"),
        new ThemePreset("pizarra", "Pizarra", true, "#26332C", "#2F3E36", "#F4F1E6", "#F6D365"),
        new ThemePreset("formal", "Formal", false, "#E6EAF0", "#F5F7FA", "#0F2747", "#A8854A"),
        new ThemePreset("elegante", "Elegante", true, "#0E0E10", "#16161A", "#F3EAD7", "#B8975A"),
        new ThemePreset("infantil", "Infantil", false, "#FFF3DC", "#FFFFFF", "#5B3A8E", "#FF6B9D"),
        new ThemePreset("juvenil", "Juvenil", true, "#1A1033", "#261A45", "#F3EEFF", "#FF5FA2"),
        new ThemePreset("synthwave", "Synthwave", true, "#1A0638", "#260A4F", "#FFE9FF", "#FF71CE"),
        new ThemePreset("terminal", "Terminal", true, "#031208", "#06260F", "#7DFFA0", "#33FF66"),
        new ThemePreset("neobrutalista", "Neobrutalista", false, "#FFF8E7", "#FFFFFF", "#111111", "#FF5C8A"),
        new ThemePreset("minimalista", "Minimalista", false, "#EFEBE4", "#FBF9F6", "#2B2723", "#EE7A3C"),
        new ThemePreset("pixel", "Pixel", true, "#0B1230", "#141C44", "#FFF1E8", "#FF004D")
    );

    public static ThemePreset byId(String id) {
        for (ThemePreset t : ALL) if (t.id.equals(id)) return t;
        return ALL.get(0);
    }

    public static boolean exists(String id) {
        for (ThemePreset t : ALL) if (t.id.equals(id)) return true;
        return false;
    }
}
