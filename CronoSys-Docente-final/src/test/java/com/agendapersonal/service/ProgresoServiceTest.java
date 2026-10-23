package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.Tarea;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgresoServiceTest {
    private static Tarea t(int id, String estado, Integer proyecto) {
        return new Tarea(id, "T" + id, "", estado, LocalDate.of(2026, 10, 20), "Media", proyecto);
    }

    @Test void sinTareasEsCero() {
        assertEquals(0, ProgresoService.porcentaje(1, List.of()));
    }

    @Test void porcentajePorProyecto() {
        List<Tarea> ts = new ArrayList<>();
        for (int i = 1; i <= 6; i++) ts.add(t(i, "Pendiente", 1));
        ts.add(t(99, "Completada", 2));                       // otro proyecto: no cuenta
        assertEquals(0, ProgresoService.porcentaje(1, ts));
        ts.get(0).setEstado("Completada");
        assertEquals(17, ProgresoService.porcentaje(1, ts));  // 1/6
        ts.get(1).setEstado("Completada");
        assertEquals(33, ProgresoService.porcentaje(1, ts));  // 2/6 → el ejemplo "17% → 33%" del documento
    }

    @Test void enProgresoNoCuentaComoCompletada() {
        List<Tarea> ts = List.of(t(1, "En progreso", 1), t(2, "Completada", 1));
        assertEquals(50, ProgresoService.porcentaje(1, ts));
    }

    @Test void sinProyectoCalculaAvanceGlobal() {
        List<Tarea> ts = List.of(t(1, "Completada", 1), t(2, "Pendiente", 2), t(3, "Pendiente", null), t(4, "Pendiente", null));
        assertEquals(25, ProgresoService.porcentaje(null, ts));
    }

    @Test void mensajeDelToast() {
        var r = new ProgresoService.Resultado(1, 17, 33, true);
        assertEquals("Tarea completada (Tesis) · Progreso: 17% → 33%", r.mensaje("Tesis"));
        assertEquals("Tarea reabierta · Progreso: 33% → 17%", new ProgresoService.Resultado(null, 33, 17, false).mensaje(null));
        assertTrue(r.cambio());
    }
}
