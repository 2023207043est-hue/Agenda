package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.Evento;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgendaServiceTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private static Evento ev(int id, String ini, String fin) {
        return new Evento(id, "E" + id, DIA, ini, fin, "", "Personal", "Media");
    }

    @Test void duracionExacta() {
        assertEquals(90, AgendaService.duracionMinutos(ev(1, "09:00", "10:30")));
        assertEquals(45, AgendaService.duracionMinutos(ev(1, "9:15", "10:00")));
        assertEquals("1 h 30 min", AgendaService.formatearMinutos(90));
        assertEquals("45 min", AgendaService.formatearMinutos(45));
        assertEquals("2 h", AgendaService.formatearMinutos(120));
    }

    @Test void validacion() {
        assertTrue(AgendaService.validar(ev(0, "09:00", "10:00")).isEmpty());
        assertTrue(AgendaService.validar(ev(0, "10:00", "09:00")).isPresent());   // fin antes que inicio
        assertTrue(AgendaService.validar(ev(0, "10:00", "10:00")).isPresent());   // duración cero
        assertTrue(AgendaService.validar(ev(0, "25:00", "26:00")).isPresent());   // hora imposible
        assertTrue(AgendaService.validar(ev(0, "9.30", "10:00")).isPresent());    // formato incorrecto
        assertTrue(AgendaService.validar(ev(0, "", "10:00")).isPresent());
        Evento sinFecha = new Evento(0, "X", null, "09:00", "10:00", "", "Personal", "Media");
        assertTrue(AgendaService.validar(sinFecha).isPresent());
    }

    @Test void cruceParcialSeCalculaEnMinutos() {
        var existentes = List.of(ev(1, "10:00", "11:00"));
        var c = AgendaService.detectarConflictos(ev(0, "10:30", "11:30"), existentes);
        assertEquals(1, c.size());
        assertEquals(30, c.getFirst().minutosSolapados());   // no basta comparar la hora de inicio
    }

    @Test void eventoContenidoYContenedor() {
        var existentes = List.of(ev(1, "09:00", "12:00"));
        assertEquals(60, AgendaService.detectarConflictos(ev(0, "10:00", "11:00"), existentes).getFirst().minutosSolapados());
        var interno = List.of(ev(1, "10:00", "11:00"));
        assertEquals(60, AgendaService.detectarConflictos(ev(0, "09:00", "12:00"), interno).getFirst().minutosSolapados());
    }

    @Test void bordesQueSoloSeTocanNoSeCruzan() {
        var existentes = List.of(ev(1, "10:00", "11:00"));
        assertTrue(AgendaService.detectarConflictos(ev(0, "11:00", "12:00"), existentes).isEmpty());
        assertTrue(AgendaService.detectarConflictos(ev(0, "09:00", "10:00"), existentes).isEmpty());
    }

    @Test void otroDiaNoCuenta() {
        Evento otro = new Evento(1, "Otro", DIA.plusDays(1), "10:00", "11:00", "", "Personal", "Media");
        assertTrue(AgendaService.detectarConflictos(ev(0, "10:00", "11:00"), List.of(otro)).isEmpty());
    }

    @Test void editarUnEventoNoChocaConsigoMismo() {
        var existentes = List.of(ev(7, "10:00", "11:00"));
        assertTrue(AgendaService.detectarConflictos(ev(7, "10:15", "11:15"), existentes).isEmpty());
    }

    @Test void datosCorruptosNoRompenLaDeteccion() {
        var existentes = List.of(ev(1, "basura", "??"), ev(2, "10:00", "11:00"));
        assertEquals(1, AgendaService.detectarConflictos(ev(0, "10:30", "10:45"), existentes).size());
    }

    @Test void minutosOcupadosNoDuplicaSolapes() {
        var evs = List.of(ev(1, "09:00", "10:00"), ev(2, "09:30", "10:30"), ev(3, "14:00", "15:00"));
        assertEquals(150, AgendaService.minutosOcupados(DIA, evs));   // 9:00–10:30 + 14:00–15:00
    }

    @Test void sugiereElPrimerHuecoLibre() {
        var evs = List.of(ev(1, "07:00", "09:00"), ev(2, "09:30", "10:00"), ev(3, "10:00", "12:00"));
        var h = AgendaService.sugerirHueco(DIA, evs, 60, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(12, 0), h[0]);      // 9:00–9:30 es muy corto para 60 min
        assertEquals(LocalTime.of(13, 0), h[1]);
        var corto = AgendaService.sugerirHueco(DIA, evs, 30, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(9, 0), corto[0]);   // sí cabe 30 min entre 9:00 y 9:30
    }

    @Test void sinHuecoCuandoElDiaEstaLleno() {
        var evs = List.of(ev(1, "08:00", "20:00"));
        assertTrue(AgendaService.sugerirHueco(DIA, evs, 60, LocalTime.of(8, 0), LocalTime.of(20, 0)).isEmpty());
    }
}
