package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecordatorioServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static Evento ev(int id, String t, LocalDate f, String hi, String hf) {
        return new Evento(id, t, f, hi, hf, "", "Personal", "Media");
    }

    private static CalendarioUnificadoService.Datos datos(List<Evento> e, List<Tarea> t, List<Contacto> c) {
        return new CalendarioUnificadoService.Datos(e, t, List.of(new Habito(1, "Leer", "", "DIARIO", 1, "#0", true)), c, Map.of());
    }

    @Test void sinNadaNoHayNoticias() {
        var r = RecordatorioService.resumenInicio(HOY, datos(List.of(), List.of(), List.of()), 7);
        assertFalse(r.hayNoticias());
        assertEquals(1, r.habitosPendientes().size());   // un hábito pendiente solo NO abre la ventana
    }

    @Test void cumpleanosDeHoyYProximosAbrenLaVentana() {
        var c = List.of(new Contacto(1, "Ana", "", "", "", "2000-10-16", "", false, null),
                        new Contacto(2, "Luis", "", "", "", "1990-10-20", "", false, null),
                        new Contacto(3, "Lejano", "", "", "", "1990-03-01", "", false, null));
        var r = RecordatorioService.resumenInicio(HOY, datos(List.of(), List.of(), c), 7);
        assertTrue(r.hayNoticias());
        assertEquals(List.of("Ana", "Luis"), r.cumples().stream().map(k -> k.contacto().getNombre()).toList());
    }

    @Test void eventosHoyManianaYTareasVencidas() {
        var e = List.of(ev(1, "Hoy", HOY, "09:00", "10:00"), ev(2, "Mañana", HOY.plusDays(1), "08:00", "09:00"),
                        ev(3, "Lejos", HOY.plusDays(5), "08:00", "09:00"));
        var t = List.of(new Tarea(1, "Vieja", "", "Pendiente", HOY.minusDays(3), "Alta"),
                        new Tarea(2, "Hecha", "", "Completada", HOY.minusDays(3), "Alta"),
                        new Tarea(3, "Hoy", "", "Pendiente", HOY, "Media"));
        var r = RecordatorioService.resumenInicio(HOY, datos(e, t, List.of()), 7);
        assertEquals(1, r.eventosHoy().size());
        assertEquals(1, r.eventosManana().size());
        assertEquals(1, r.tareasVencidas().size());   // la completada no cuenta
        assertEquals(1, r.tareasHoy().size());
    }

    @Test void avisaSoloDentroDeLaVentanaYUnaVez() {
        var d = datos(List.of(ev(1, "Dentista", HOY, "10:00", "11:00")), List.of(), List.of());
        Set<String> avisados = new HashSet<>();
        assertTrue(RecordatorioService.avisosEventos(HOY.atTime(9, 30), d, 15, avisados).isEmpty());   // faltan 30
        var a = RecordatorioService.avisosEventos(HOY.atTime(9, 50), d, 15, avisados);
        assertEquals(1, a.size());
        assertTrue(a.getFirst().mensaje().startsWith("Empieza en 10 min"));
        avisados.add(a.getFirst().clave());
        assertTrue(RecordatorioService.avisosEventos(HOY.atTime(9, 52), d, 15, avisados).isEmpty());  // ya avisado
        assertFalse(RecordatorioService.avisosEventos(HOY.atTime(10, 0), d, 15, new HashSet<>()).isEmpty()); // "ahora"
        assertTrue(RecordatorioService.avisosEventos(HOY.atTime(10, 5), d, 15, new HashSet<>()).isEmpty());  // ya pasó
    }

    @Test void avisoDeCumpleHoyNoSeRepite() {
        var d = datos(List.of(), List.of(), List.of(new Contacto(7, "Ana", "", "", "", "2000-10-16", "", false, null)));
        Set<String> av = new HashSet<>();
        var a = RecordatorioService.avisosCumpleHoy(HOY, d, av);
        assertEquals(1, a.size());
        av.add(a.getFirst().clave());
        assertTrue(RecordatorioService.avisosCumpleHoy(HOY, d, av).isEmpty());
    }
}
