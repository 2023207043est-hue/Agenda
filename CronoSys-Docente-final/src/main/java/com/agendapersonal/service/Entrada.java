package com.agendapersonal.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Elemento mostrable en el calendario unificado. {@code origen} es el objeto real
 * (Evento, Tarea, Habito o Contacto) para poder editarlo al hacer clic.
 * Si {@code inicio} es null, es un elemento de "todo el día".
 */
public record Entrada(TipoEntrada tipo, String titulo, String detalle, LocalDate fecha,
                      LocalTime inicio, LocalTime fin, boolean hecha, Object origen) {

    public boolean todoElDia() { return inicio == null; }

    /** Minuto de inicio (0..1439); solo válido si no es de todo el día. */
    public int minutoInicio() { return inicio.getHour() * 60 + inicio.getMinute(); }

    /** Minuto de fin; si falta o es anterior/igual al inicio, se asume 30 min mínimos. */
    public int minutoFin() {
        int ini = minutoInicio();
        int f = fin == null ? ini + 30 : fin.getHour() * 60 + fin.getMinute();
        return Math.max(f, ini + 30);
    }
}
