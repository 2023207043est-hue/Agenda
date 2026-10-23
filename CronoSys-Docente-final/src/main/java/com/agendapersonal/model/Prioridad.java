package com.agendapersonal.model;

import java.text.Normalizer;
import java.util.Locale;

/** Prioridad cerrada: el sistema no admite valores fuera de este conjunto. */
public enum Prioridad {
    BAJA("Baja", 1), MEDIA("Media", 2), ALTA("Alta", 3);

    private final String etiqueta;
    private final int peso;

    Prioridad(String etiqueta, int peso) { this.etiqueta = etiqueta; this.peso = peso; }

    /** Texto mostrado en la UI y guardado en SQLite (compatible con datos existentes). */
    public String etiqueta() { return etiqueta; }
    /** Nombre del color del tema (CSS): rojo = alta, naranja = media, verde = baja. */
    public String color() { return switch (this) { case ALTA -> "-p-alta"; case MEDIA -> "-p-media"; case BAJA -> "-p-baja"; }; }
    /** Fondo suave del mismo color. */
    public String fondo() { return color() + "-soft"; }
    /** Sufijo de la clase CSS de la etiqueta ({@code pill-p-alta}…). */
    public String tono() { return switch (this) { case ALTA -> "p-alta"; case MEDIA -> "p-media"; case BAJA -> "p-baja"; }; }
    /** Peso numérico (1..3) útil para ordenar y puntuar. */
    public int peso() { return peso; }

    /** Parseo estricto e insensible a mayúsculas/acentos. Null o vacío → MEDIA. */
    public static Prioridad desde(String texto) {
        if (texto == null || texto.isBlank()) return MEDIA;
        String t = normalizar(texto);
        for (Prioridad p : values())
            if (normalizar(p.etiqueta).equals(t) || p.name().equalsIgnoreCase(t)) return p;
        throw new IllegalArgumentException("Prioridad no válida: '" + texto + "' (use Baja, Media o Alta)");
    }

    /** Parseo tolerante para datos ya guardados: ante basura devuelve MEDIA. */
    public static Prioridad desdeBD(String texto) {
        try { return desde(texto); } catch (IllegalArgumentException e) { return MEDIA; }
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
