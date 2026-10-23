package com.agendapersonal.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.Prioridad;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RelojPrefsTest {
    @Test void formato24ConYSinSegundos() {
        assertEquals("09:05", RelojPrefs.formatear(LocalTime.of(9, 5, 30), true, false));
        assertEquals("21:07:09", RelojPrefs.formatear(LocalTime.of(21, 7, 9), true, true));
    }

    @Test void formato12HorasUsaAmPm() {
        String s = RelojPrefs.formatear(LocalTime.of(21, 7), false, false).toLowerCase();
        assertTrue(s.startsWith("9:07"), s);
        assertTrue(s.contains("p"), s);
    }

    @Test void tamanoDesconocidoCaeEnMediano() {
        assertEquals(RelojPrefs.Tamano.MEDIANO, RelojPrefs.Tamano.desde("loquesea"));
        assertEquals(RelojPrefs.Tamano.GRANDE, RelojPrefs.Tamano.desde("GRANDE"));
    }

    @Test void prioridadTieneUnColorDistintoPorNivel() {
        assertEquals("-p-alta", Prioridad.ALTA.color());
        assertEquals("-p-media", Prioridad.MEDIA.color());
        assertEquals("-p-baja-soft", Prioridad.BAJA.fondo());
        assertEquals("p-media", Prioridad.MEDIA.tono());
    }
}
