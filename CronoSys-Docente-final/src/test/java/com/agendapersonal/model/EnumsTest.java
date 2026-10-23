package com.agendapersonal.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EnumsTest {
    @Test void prioridadAceptaVariantesPeroRechazaBasura() {
        assertEquals(Prioridad.ALTA, Prioridad.desde("Alta"));
        assertEquals(Prioridad.ALTA, Prioridad.desde(" alta "));
        assertEquals(Prioridad.MEDIA, Prioridad.desde(null));
        assertThrows(IllegalArgumentException.class, () -> Prioridad.desde("Urgentísima"));
        assertEquals(Prioridad.MEDIA, Prioridad.desdeBD("???"));   // datos viejos no rompen la app
    }

    @Test void estadoTarea() {
        assertEquals(EstadoTarea.EN_PROGRESO, EstadoTarea.desde("En progreso"));
        assertEquals(EstadoTarea.EN_PROGRESO, EstadoTarea.desde("EN_PROGRESO"));
        assertEquals("Completada", EstadoTarea.COMPLETADA.etiqueta());
        assertThrows(IllegalArgumentException.class, () -> EstadoTarea.desde("Hecha"));
        assertEquals(EstadoTarea.PENDIENTE, EstadoTarea.desdeBD("x"));
    }

    @Test void modelosValidanAlAsignar() {
        Tarea t = new Tarea("T", "", "Pendiente", LocalDate.now(), "Alta");
        assertEquals("Alta", t.getPrioridad());
        assertThrows(IllegalArgumentException.class, () -> t.setEstado("Inventado"));
        assertEquals("Pendiente", t.getEstado());                  // no quedó en estado inválido
        Evento e = new Evento("E", LocalDate.now(), "09:00", "10:00", "", "Personal", "baja");
        assertEquals("Baja", e.getPrioridad());                    // normalizada
        assertThrows(IllegalArgumentException.class, () -> e.setPrioridad("X"));
    }

    @Test void horasTipadas() {
        assertTrue(Horas.esValida("09:30"));
        assertTrue(Horas.esValida("9:30"));
        assertFalse(Horas.esValida("24:00"));
        assertFalse(Horas.esValida("10/13"));
        assertFalse(Horas.esValida(null));
    }
}
