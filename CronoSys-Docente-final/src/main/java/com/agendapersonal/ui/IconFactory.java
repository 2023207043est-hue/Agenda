package com.agendapersonal.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Íconos vectoriales de línea dibujados por código (cuadrícula de 24×24, trazo redondeado).
 *
 * <p>Se pintan con CSS (clase {@code .cs-icon}), así que toman el color del tema activo y cambian
 * solos al pasar de claro a oscuro o al cambiar el color de acento. Se ven nítidos a cualquier tamaño.
 * Para teñir un ícono basta con añadirle una clase: {@code tint-accent}, {@code tint-blue},
 * {@code tint-green}, {@code tint-on-accent}, etc. (ver styles.css).</p>
 */
public final class IconFactory {
    private static final ReadOnlyBooleanWrapper DARK = new ReadOnlyBooleanWrapper(false);
    private static final Map<String, String> PATHS = new HashMap<>();
    private static final Map<String, String> ALIAS = Map.ofEntries(
        Map.entry("time", "clock"), Map.entry("event", "calendar-check"), Map.entry("sleep", "moon"),
        Map.entry("project", "folder"), Map.entry("vivienda", "home"), Map.entry("salary", "work"),
        Map.entry("arrow-back", "arrow-left"), Map.entry("logo", "clock")
    );

    private IconFactory() {}

    public static ReadOnlyBooleanProperty darkModeProperty() { return DARK.getReadOnlyProperty(); }
    public static void setDarkMode(boolean dark) { DARK.set(dark); }

    /** Ícono de línea del tamaño indicado (en px). Se mantiene el nombre del método anterior. */
    public static Node image(String name, double size) { return icon(name, size); }

    public static Node icon(String name, double size, String... styleClasses) {
        String key = name == null ? "info" : name.toLowerCase(Locale.ROOT);
        key = ALIAS.getOrDefault(key, key);
        String d = PATHS.getOrDefault(key, PATHS.get("dot"));
        double k = size / 24.0;

        Rectangle box = new Rectangle(24, 24);           // fija la caja aunque el trazo no la llene
        box.setFill(null);
        box.setStroke(null);
        box.setMouseTransparent(true);
        SVGPath path = new SVGPath();
        path.setContent(d);
        path.getStyleClass().add("icon-path");
        Scale s = new Scale(k, k, 0, 0);
        box.getTransforms().add(s);
        path.getTransforms().add(s);

        Group g = new Group(box, path);
        g.getStyleClass().add("cs-icon");
        if (FILLED.contains(key)) g.getStyleClass().add("icon-fill");
        g.getStyleClass().addAll(styleClasses);
        g.setMouseTransparent(true);
        g.setAccessibleText(name);
        return g;
    }

    /** ¿Existe un ícono con este nombre? */
    public static boolean exists(String name) {
        return name != null && PATHS.containsKey(ALIAS.getOrDefault(name.toLowerCase(Locale.ROOT), name.toLowerCase(Locale.ROOT)));
    }

    // ------------------------------------------------------------------ dibujos

    private static final java.util.Set<String> FILLED = java.util.Set.of("more", "dot", "favorite-on");

    private static String c(double cx, double cy, double r) {
        return String.format(Locale.ROOT, "M%.2f,%.2f a%.2f,%.2f 0 1,0 %.2f,0 a%.2f,%.2f 0 1,0 %.2f,0 ",
            cx - r, cy, r, r, 2 * r, r, r, -2 * r);
    }

    private static String rr(double x, double y, double w, double h, double r) {
        return String.format(Locale.ROOT,
            "M%.2f,%.2f H%.2f A%.2f,%.2f 0 0 1 %.2f,%.2f V%.2f A%.2f,%.2f 0 0 1 %.2f,%.2f H%.2f "
                + "A%.2f,%.2f 0 0 1 %.2f,%.2f V%.2f A%.2f,%.2f 0 0 1 %.2f,%.2f Z ",
            x + r, y, x + w - r, r, r, x + w, y + r, y + h - r, r, r, x + w - r, y + h, x + r,
            r, r, x, y + h - r, y + r, r, r, x + r, y);
    }

    private static void p(String name, String d) { PATHS.put(name, d); }

    static {
        String cal = rr(3.5, 5, 17, 15.5, 3) + "M8 3v4 M16 3v4 M3.5 10h17 ";
        p("home", "M3 10.5 L12 3 L21 10.5 M5.5 9 V20.5 H18.5 V9 M10 20.5 V14.5 H14 V20.5");
        p("calendar", cal);
        p("calendar-check", cal + "M9 15 l2 2 4-4");
        p("reschedule", cal + "M8.5 15.5 h6.5 M13 13.5 l2 2 -2 2");
        p("agenda", rr(3.5, 4, 17, 16.5, 3) + "M8 9 h8 M8 13 h8 M8 17 h5");
        p("clock", c(12, 12, 8.5) + "M12 7.5 V12 l3 2");
        p("tasks", "M4 7 l1.8 1.8 L9 5.5 M4 15 l1.8 1.8 L9 13.5 M12.5 7.5 H20 M12.5 15.5 H20");
        p("notes", "M14 3.5 H7 a2.5 2.5 0 0 0 -2.5 2.5 v12 A2.5 2.5 0 0 0 7 20.5 h10 a2.5 2.5 0 0 0 2.5 -2.5 V9 z "
            + "M14 3.5 V9 h5.5 M8.5 13 h7 M8.5 16.5 h4.5");
        p("contacts", c(12, 8.5, 4) + "M4.5 20.5 a7.5 7.5 0 0 1 15 0");
        p("habits", "M17 3 l3.5 3.5 L17 10 M3.5 11.5 V10 A3.5 3.5 0 0 1 7 6.5 h13.5 "
            + "M7 21 l-3.5 -3.5 L7 14 M20.5 12.5 V14 a3.5 3.5 0 0 1 -3.5 3.5 H3.5");
        p("finance", "M4 7.5 v-1 A2.5 2.5 0 0 1 6.5 4 H17 " + rr(3.5, 7.5, 17, 12.5, 3) + "M15.5 13.75 h2");
        p("health", "M12 20 s-7.5 -4.6 -7.5 -10.2 A4.3 4.3 0 0 1 12 7 a4.3 4.3 0 0 1 7.5 2.8 C19.5 15.4 12 20 12 20 z");
        p("settings", "M4 7 h9 M17 7 h3 M4 12 h3 M11 12 h9 M4 17 h11 M19 17 h1 " + c(15, 7, 2) + c(9, 12, 2) + c(17, 17, 2));
        p("add", "M12 5 v14 M5 12 h14");
        p("check", "M5 12.5 l4.5 4.5 L19 7.5");
        p("close", "M6 6 l12 12 M18 6 L6 18");
        p("power", "M12 3.5 v8 M7 6.8 a7.5 7.5 0 1 0 10 0");
        p("edit", "M4 20 h4 L19 9 l-4 -4 L4 16 z M13.5 6.5 l4 4");
        p("delete", "M4.5 7 h15 M10 11 v6 M14 11 v6 M6.5 7 l.8 12 a1.5 1.5 0 0 0 1.5 1.5 h6.4 a1.5 1.5 0 0 0 1.5 -1.5 l.8 -12 M9.5 7 V4.5 h5 V7");
        p("search", c(11, 11, 6.5) + "M20 20 l-4.2 -4.2");
        p("favorite", "M12 3.8 l2.5 5.1 5.6 .8 -4 3.9 .9 5.6 -5 -2.6 -5 2.6 .9 -5.6 -4 -3.9 5.6 -.8 z");
        p("favorite-on", PATHS.get("favorite"));
        p("focus", c(12, 12, 8.5) + c(12, 12, 4.5) + "M12 11.9 v.2");
        p("work", rr(3.5, 7.5, 17, 12.5, 2.5) + "M9 7.5 V6 a2 2 0 0 1 2 -2 h2 a2 2 0 0 1 2 2 v1.5 M3.5 13 h17");
        p("birthday", "M4 20.5 h16 M5 20.5 v-6 a2 2 0 0 1 2 -2 h10 a2 2 0 0 1 2 2 v6 "
            + "M5 16.4 c2.3 0 2.3 -1.4 4.6 -1.4 s2.4 1.4 4.7 1.4 2.4 -1.4 4.7 -1.4 M12 12.5 V9 "
            + "M12 6.6 c-.8 0 -1.3 -.6 -1.3 -1.3 0 -.9 1.3 -2.2 1.3 -2.2 s1.3 1.3 1.3 2.2 c0 .7 -.5 1.3 -1.3 1.3 z");
        p("notification", "M6 16.5 V11 a6 6 0 0 1 12 0 v5.5 l1.5 2 H4.5 z M10 20.5 a2 2 0 0 0 4 0");
        p("idea", "M9 18 h6 M10 21 h4 M12 3 a6 6 0 0 0 -3.5 10.9 c.6 .5 1 1.2 1 2.1 h5 c0 -.9 .4 -1.6 1 -2.1 A6 6 0 0 0 12 3 z");
        p("folder", "M3.5 7.5 a2 2 0 0 1 2 -2 h4 l2 2.5 h7 a2 2 0 0 1 2 2 v8 a2 2 0 0 1 -2 2 h-13 a2 2 0 0 1 -2 -2 z");
        p("arrow-left", "M19 12 H5 M11 6 l-6 6 6 6");
        p("arrow-right", "M5 12 h14 M13 6 l6 6 -6 6");
        p("chevron-down", "M7 10 l5 5 5 -5");
        p("chevron-up", "M7 14 l5 -5 5 5");
        p("chevron-left", "M14.5 6 l-6 6 6 6");
        p("chevron-right", "M9.5 6 l6 6 -6 6");
        p("sidebar-collapse", rr(3.5, 4.5, 17, 15, 3) + "M9.5 4.5 v15 M15.5 9.5 l-2.5 2.5 2.5 2.5");
        p("sidebar-expand", rr(3.5, 4.5, 17, 15, 3) + "M9.5 4.5 v15 M13 9.5 l2.5 2.5 -2.5 2.5");
        p("sun", c(12, 12, 4) + "M12 2.5 v2 M12 19.5 v2 M4.6 4.6 l1.4 1.4 M18 18 l1.4 1.4 M2.5 12 h2 M19.5 12 h2 M4.6 19.4 L6 18 M18 6 l1.4 -1.4");
        p("moon", "M19.5 14.5 A7.5 7.5 0 1 1 9.5 4.5 a6 6 0 0 0 10 10 z");
        p("more", c(5.5, 12, 1.3) + c(12, 12, 1.3) + c(18.5, 12, 1.3));
        p("dot", c(12, 12, 3));
        p("flame", "M12 21 c3.9 0 6.5 -2.6 6.5 -6.3 0 -3.8 -2.8 -5.7 -3.9 -8.7 -.9 1.8 -1.8 2.8 -3.2 3.3 "
            + ".3 -2.3 -.5 -4.6 -2.4 -6.3 -.6 3.8 -4.5 6.4 -4.5 11.2 C4.5 18 7.8 21 12 21 z");
        p("water", "M12 3.5 s6 6.4 6 10.8 a6 6 0 0 1 -12 0 C6 9.9 12 3.5 12 3.5 z");
        p("activity", "M3.5 12 h3.5 l2.5 -6.5 5 13 2.5 -6.5 h3.5");
        p("scale", rr(3.5, 3.5, 17, 17, 4) + "M8.5 9 a4.5 4.5 0 0 1 7 0 M12 10.5 l1.5 -2");
        p("mood", c(12, 12, 8.5) + "M8.5 14 a4.5 4.5 0 0 0 7 0 M9.5 9.5 h.01 M14.5 9.5 h.01");
        p("phone", "M6 3.5 h3 l1.8 4.5 -2.3 1.4 a11 11 0 0 0 6.1 6.1 l1.4 -2.3 4.5 1.8 v3 a2 2 0 0 1 -2 2 A16.5 16.5 0 0 1 4 5.5 a2 2 0 0 1 2 -2 z");
        p("mail", rr(3.5, 5.5, 17, 13, 2.5) + "M4 7.5 l8 5.5 8 -5.5");
        p("tag", "M3.5 12 V4.5 h7.5 l9 9 -7.5 7.5 z M8 8.5 h.01");
        p("inbox", "M3.5 13.5 h5 l1.5 2.5 h4 l1.5 -2.5 h5 M5.5 5.5 h13 l2 8 v5 a2 2 0 0 1 -2 2 h-13 a2 2 0 0 1 -2 -2 v-5 z");
        p("food", "M6.5 3.5 v6 a2 2 0 0 0 4 0 v-6 M8.5 9.5 v11 M17 3.5 c-2 1.5 -2.5 4.5 -2.5 6.5 s1 3 2.5 3 v7.5");
        p("transport", "M5 16.5 V12 l2 -5.5 h10 l2 5.5 v4.5 M3.5 12 h17 M5 16.5 h14 M7 16.5 v2.5 M17 16.5 v2.5 M7.5 14 h.01 M16.5 14 h.01");
        p("leisure", rr(3.5, 4.5, 17, 15, 2.5) + "M8 4.5 v15 M16 4.5 v15 M3.5 9.5 H8 M3.5 14.5 H8 M16 9.5 h4.5 M16 14.5 h4.5");
        p("sparkle", "M12 3.5 l1.9 5.1 5.1 1.9 -5.1 1.9 L12 17.5 l-1.9 -5.1 L5 10.5 l5.1 -1.9 z M18.5 16 v4 M16.5 18 h4");
        p("bolt", "M13 3 L5 13.5 h6 L10 21 l8 -10.5 h-6 z");
        p("arrow-up-right", "M7 17 L17 7 M9 7 h8 v8");
        p("arrow-down-left", "M17 7 L7 17 M15 17 H7 V9");
        p("info", c(12, 12, 8.5) + "M12 11 v5.5 M12 7.8 h.01");
        p("warning", "M12 4 l9 15.5 H3 z M12 10 v4 M12 17 h.01");
        p("success", c(12, 12, 8.5) + "M8 12.3 l2.7 2.7 L16 9.6");
        p("error", c(12, 12, 8.5) + "M9 9 l6 6 M15 9 l-6 6");
        p("refresh", "M20 11 a8 8 0 0 0 -14.6 -4.5 L3.5 9 M3.5 4 v5 h5 M4 13 a8 8 0 0 0 14.6 4.5 l1.9 -2.5 M20.5 20 v-5 h-5");
        p("book", "M12 6.5 C10 5 7 4.6 3.5 5 v13 c3.5 -.4 6.5 0 8.5 1.5 c2 -1.5 5 -1.9 8.5 -1.5 v-13 c-3.5 -.4 -6.5 0 -8.5 1.5 z M12 6.5 v13");
        p("filter", "M4 5.5 h16 l-6.2 7.3 v5.7 l-3.6 2 v-7.7 z");
        p("pause", "M9 6 v12 M15 6 v12");
        p("play", "M8 5.5 v13 l10.5 -6.5 z");
        p("grid", rr(3.5, 3.5, 7, 7, 2) + rr(13.5, 3.5, 7, 7, 2) + rr(3.5, 13.5, 7, 7, 2) + rr(13.5, 13.5, 7, 7, 2));
        p("shopping", "M5 7.5 h14 l-1.2 11 a2 2 0 0 1 -2 1.8 H8.2 a2 2 0 0 1 -2 -1.8 z M9 7.5 V6.5 a3 3 0 0 1 6 0 v1");
        p("medical", rr(3.5, 3.5, 17, 17, 4) + "M12 8 v8 M8 12 h8");
        p("coins", c(9, 9, 5.5) + "M14.6 8.6 a5.5 5.5 0 1 1 -6 6");
        p("user-check", c(10, 8.5, 4) + "M3 20.5 a7 7 0 0 1 12.5 -4.3 M15.5 18.5 l2 2 3.5 -4");
        p("link", "M10 14 a4 4 0 0 0 5.7 0 l3 -3 a4 4 0 0 0 -5.7 -5.7 l-1 1 M14 10 a4 4 0 0 0 -5.7 0 l-3 3 a4 4 0 0 0 5.7 5.7 l1 -1");
        p("list", "M9 6.5 h11 M9 12 h11 M9 17.5 h11 M4.5 6.5 h.01 M4.5 12 h.01 M4.5 17.5 h.01");
        p("copy", rr(3.5, 3.5, 12.5, 12.5, 2.5) + rr(8, 8, 12.5, 12.5, 2.5));
        p("music", "M9 18 V6 l10 -2 v12 M9 18 a2.5 2.5 0 1 1 -5 0 a2.5 2.5 0 1 1 5 0 M19 16 a2.5 2.5 0 1 1 -5 0 a2.5 2.5 0 1 1 5 0");
    }
}
