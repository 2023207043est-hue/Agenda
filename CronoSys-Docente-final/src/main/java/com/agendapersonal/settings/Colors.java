package com.agendapersonal.settings;

import java.util.Locale;

/** Utilidades de color sin dependencias de JavaFX: parseo hex, luminancia, contraste y mezcla. */
public final class Colors {
    private Colors() {}

    /** Devuelve "#RRGGBB" en mayúsculas o null si no es un hex válido (#RGB o #RRGGBB). */
    public static String normalize(String hex) {
        if (hex == null) return null;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.matches("[0-9a-fA-F]{3}")) h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        if (!h.matches("[0-9a-fA-F]{6}")) return null;
        return "#" + h.toUpperCase(Locale.ROOT);
    }

    public static int[] rgb(String hex) {
        String n = normalize(hex);
        if (n == null) n = "#000000";
        return new int[]{ Integer.parseInt(n.substring(1, 3), 16), Integer.parseInt(n.substring(3, 5), 16), Integer.parseInt(n.substring(5, 7), 16) };
    }

    public static String hex(int r, int g, int b) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Luminancia relativa WCAG (0 a 1). */
    public static double luminance(String hex) {
        int[] c = rgb(hex);
        double[] l = new double[3];
        for (int k = 0; k < 3; k++) {
            double v = c[k] / 255.0;
            l[k] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
    }

    public static boolean isDark(String hex) { return luminance(hex) < 0.35; }

    /** Razón de contraste WCAG (1 a 21). */
    public static double contrast(String a, String b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Texto sobre un color de fondo (por ejemplo el acento): el de mayor contraste entre claro y oscuro. */
    public static String onColor(String bg) {
        String dark = "#16110E", light = "#FFFFFF";
        return contrast(bg, dark) >= contrast(bg, light) ? dark : light;
    }

    /** Mezcla: t=0 devuelve a, t=1 devuelve b. */
    public static String mix(String a, String b, double t) {
        int[] x = rgb(a), y = rgb(b);
        return hex((int) Math.round(x[0] + (y[0] - x[0]) * t), (int) Math.round(x[1] + (y[1] - x[1]) * t), (int) Math.round(x[2] + (y[2] - x[2]) * t));
    }
}
