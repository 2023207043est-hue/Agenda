package com.agendapersonal.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Conversión estricta de "HH:mm" a {@link LocalTime} (rechaza "25:00", "9.30", "", null...). */
public final class Horas {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("HH:mm");
    private Horas() {}

    public static LocalTime parsear(String texto) {
        if (texto == null) throw new DateTimeParseException("Hora vacía", "", 0);
        return LocalTime.parse(texto.trim(), FMT);
    }

    public static boolean esValida(String texto) {
        try { parsear(texto); return true; } catch (DateTimeParseException e) { return false; }
    }

    /** "9:5" no es válido; "9:05" → "09:05". Si el texto no es una hora válida lo devuelve igual (la validación lo rechaza). */
    public static String normalizar(String texto) {
        return esValida(texto) ? parsear(texto).format(SALIDA) : texto;
    }
}
