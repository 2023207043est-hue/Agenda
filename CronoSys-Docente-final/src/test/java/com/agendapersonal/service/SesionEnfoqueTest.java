package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SesionEnfoqueTest {
    @Test void pomodoroCuatroSesionesYDescansoLargo() {
        SesionEnfoque s = new SesionEnfoque(MetodoEnfoque.PREDEFINIDOS.get(0));
        s.iniciar();
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, s.sesion());
            assertTrue(s.tick(25 * 60));
            assertEquals(SesionEnfoque.Fase.DESCANSO, s.fase());
            assertTrue(s.tick(5 * 60));
        }
        assertEquals(4, s.sesion());
        assertTrue(s.tick(25 * 60));
        assertEquals(SesionEnfoque.Fase.DESCANSO_LARGO, s.fase());
        // El descanso largo del pomodoro es un rango de 15 a 30 min.
        assertTrue(s.total() >= 15 * 60 && s.total() <= 30 * 60, "descanso largo fuera del rango: " + s.total());
        assertTrue(s.tick(s.total()));
        assertEquals(1, s.sesion());
        assertEquals(2, s.ciclo());
    }

    @Test void pausaYReinicio() {
        SesionEnfoque s = new SesionEnfoque(MetodoEnfoque.PREDEFINIDOS.get(1));
        assertFalse(s.tick(60));               // empieza en pausa
        s.iniciar();
        s.tick(600);
        assertEquals(600, s.transcurrido());
        s.pausar();
        s.tick(600);
        assertEquals(600, s.transcurrido());
        s.reiniciarFase();
        assertEquals(0, s.transcurrido());
        assertEquals("52:00", SesionEnfoque.formato(s.restantes()));
    }

    @Test void flowtimeCalculaElDescanso() {
        MetodoEnfoque flow = MetodoEnfoque.porId("flowtime", List.of());
        SesionEnfoque s = new SesionEnfoque(flow);
        s.iniciar();
        assertFalse(s.tick(50 * 60));
        assertTrue(s.cuentaLibre());
        s.terminarFlow();
        assertEquals(SesionEnfoque.Fase.DESCANSO, s.fase());
        assertEquals(10 * 60, s.total());
    }

    @Test void personalizadoYGuardado() {
        MetodoEnfoque m = MetodoEnfoque.personalizado("Mi método", 40, 8, 3, 25);
        List<MetodoEnfoque> l = MetodoEnfoque.desdeMapas(List.of(m.aMapa(), "basura", java.util.Map.of("name", "x")));
        assertEquals(1, l.size());
        assertEquals(40, l.get(0).trabajo());
        assertEquals(m.id(), MetodoEnfoque.porId(m.id(), l).id());
        assertEquals("pomodoro", MetodoEnfoque.porId("no-existe", l).id());
    }

    @Test void losRangosSeGuardanYSeRecuperan() {
        MetodoEnfoque m = MetodoEnfoque.personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        assertTrue(m.rangoTrabajo());
        assertTrue(m.rangoDescanso());
        assertFalse(m.rangoDescansoLargo());
        assertTrue(m.tieneRangos());
        assertEquals("60–90 min trabajo · 10–20 min descanso", m.resumen());

        List<MetodoEnfoque> l = MetodoEnfoque.desdeMapas(List.of(m.aMapa()));
        assertEquals(1, l.size());
        assertEquals(60, l.get(0).trabajoMin());
        assertEquals(90, l.get(0).trabajoMax());
        assertEquals(10, l.get(0).descansoMin());
        assertEquals(20, l.get(0).descansoMax());
    }

    @Test void laSesionEligeUnValorDentroDelRango() {
        MetodoEnfoque m = MetodoEnfoque.personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        SesionEnfoque s = new SesionEnfoque(m);
        assertTrue(s.trabajoUsado() >= 60 && s.trabajoUsado() <= 90, "trabajo fuera del rango: " + s.trabajoUsado());
        assertTrue(s.descansoUsado() >= 10 && s.descansoUsado() <= 20, "descanso fuera del rango: " + s.descansoUsado());
        assertEquals(s.trabajoUsado() * 60, s.total());
    }

    @Test void sePuedeFijarLaDuracionALaFuerza() {
        MetodoEnfoque m = MetodoEnfoque.personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        SesionEnfoque s = new SesionEnfoque(m, 75, 15, null);
        assertEquals(75, s.trabajoUsado());
        assertEquals(15, s.descansoUsado());
        assertEquals(75 * 60, s.total());
    }

    @Test void losMetodosPredefinidosCubrenLoPedido() {
        List<String> ids = MetodoEnfoque.PREDEFINIDOS.stream().map(MetodoEnfoque::id).toList();
        assertTrue(ids.contains("pomodoro"));
        assertTrue(ids.contains("52-17"));
        assertTrue(ids.contains("deep-60"));
        assertTrue(ids.contains("deep-90"));
        assertTrue(ids.contains("deep-rango"));
        assertTrue(ids.contains("ultradiano"));
        assertTrue(ids.contains("flowtime"));
        assertTrue(ids.contains("timeboxing"));
        for (MetodoEnfoque m : MetodoEnfoque.PREDEFINIDOS) {
            assertFalse(m.descripcion().isBlank(), "sin descripción: " + m.id());
            assertFalse(m.usoComun().isBlank(), "sin uso común: " + m.id());
        }
    }
}
