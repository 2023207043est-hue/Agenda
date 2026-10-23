package com.agendapersonal.model;

/** Estados cerrados de una tarea. */
public enum EstadoTarea {
    PENDIENTE("Pendiente"), EN_PROGRESO("En progreso"), COMPLETADA("Completada");

    private final String etiqueta;

    EstadoTarea(String etiqueta) { this.etiqueta = etiqueta; }

    public String etiqueta() { return etiqueta; }

    public boolean esCompletada() { return this == COMPLETADA; }

    /** Parseo estricto e insensible a mayúsculas/acentos. Null o vacío → PENDIENTE. */
    public static EstadoTarea desde(String texto) {
        if (texto == null || texto.isBlank()) return PENDIENTE;
        String t = Prioridad.normalizar(texto);
        for (EstadoTarea e : values())
            if (Prioridad.normalizar(e.etiqueta).equals(t) || Prioridad.normalizar(e.name()).equals(t)) return e;
        throw new IllegalArgumentException(
            "Estado no válido: '" + texto + "' (use Pendiente, En progreso o Completada)");
    }

    /** Parseo tolerante para datos ya guardados: ante basura devuelve PENDIENTE. */
    public static EstadoTarea desdeBD(String texto) {
        try { return desde(texto); } catch (IllegalArgumentException e) { return PENDIENTE; }
    }
}
