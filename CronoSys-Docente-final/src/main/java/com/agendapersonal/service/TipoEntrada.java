package com.agendapersonal.service;

/** Segmentos del calendario unificado (equivalente a los "Mis calendarios" de Google). */
public enum TipoEntrada {
    // Los colores son nombres de la paleta del tema (se resuelven en CSS), así se ven bien en claro y oscuro.
    EVENTO("Eventos", "-t-evento", "-t-evento-soft", "event"),
    TRABAJO("Trabajos", "-t-trabajo", "-t-trabajo-soft", "work"),
    TAREA("Tareas", "-t-tarea", "-t-tarea-soft", "tasks"),
    HABITO("Hábitos", "-t-habito", "-t-habito-soft", "habits"),
    CUMPLE("Cumpleaños", "-t-cumple", "-t-cumple-soft", "birthday");

    private final String etiqueta, color, fondo, icono;

    TipoEntrada(String etiqueta, String color, String fondo, String icono) {
        this.etiqueta = etiqueta; this.color = color; this.fondo = fondo; this.icono = icono;
    }

    public String etiqueta() { return etiqueta; }
    public String color() { return color; }
    public String fondo() { return fondo; }
    public String icono() { return icono; }
}
