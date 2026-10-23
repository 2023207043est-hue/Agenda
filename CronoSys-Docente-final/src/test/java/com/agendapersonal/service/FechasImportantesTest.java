package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FechasImportantesTest {
    @Test void idaYVuelta() {
        var l = List.of(new FechasImportantes.Fecha("Aniversario", LocalDate.of(2020, 5, 3)), new FechasImportantes.Fecha("Graduación = 1; x", LocalDate.of(2019, 12, 1)));
        String s = FechasImportantes.escribir(l);
        var r = FechasImportantes.leer(s);
        assertEquals(2, r.size());
        assertEquals("Aniversario", r.get(0).etiqueta());
        assertEquals(LocalDate.of(2019, 12, 1), r.get(1).fecha());
    }
    @Test void tolerante() {
        assertTrue(FechasImportantes.leer(null).isEmpty());
        assertTrue(FechasImportantes.leer("basura;x=y;=2020-01-01").isEmpty());
    }
    @Test void diasHasta() {
        assertEquals(0L, FechasImportantes.diasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 10)));
        assertEquals(364L, FechasImportantes.diasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 11)));
    }
}
