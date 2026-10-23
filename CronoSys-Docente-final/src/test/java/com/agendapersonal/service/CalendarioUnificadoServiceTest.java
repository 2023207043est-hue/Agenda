package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarioUnificadoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);
    private static final Set<TipoEntrada> TODO = EnumSet.allOf(TipoEntrada.class);

    private static Evento ev(int id, String t, String cat, String hi, String hf) {
        return new Evento(id, t, HOY, hi, hf, "", cat, "Media");
    }

    private static CalendarioUnificadoService.Datos datos() {
        var tareaSuelta = new Tarea(1, "Pagar luz", "", "Pendiente", HOY, "Media", null);
        var tareaProyecto = new Tarea(2, "Entregar informe", "", "Completada", HOY, "Alta", 7);
        var habDiario = new Habito(1, "Leer", "", "DIARIO", 1, "#16a34a", true);
        var habSemanal = new Habito(2, "Revisión semanal", "", "SEMANAL", 1, "#16a34a", true);
        var habInactivo = new Habito(3, "Viejo", "", "DIARIO", 1, "#16a34a", false);
        var cumple = new Contacto(5, "Ana", "", "", "Amigos", "2000-10-16", "", false, null);
        return new CalendarioUnificadoService.Datos(
            List.of(ev(1, "Dentista", "Salud", "09:00", "10:00"), ev(2, "Reunión", "Trabajo", "11:00", "12:00")),
            List.of(tareaSuelta, tareaProyecto), List.of(habDiario, habSemanal, habInactivo), List.of(cumple),
            Map.of(HOY.toString(), Set.of(1)));
    }

    @Test void segmentaCadaTipo() {
        var r = CalendarioUnificadoService.delDia(HOY, datos(), TODO);
        assertEquals(1, r.stream().filter(e -> e.tipo() == TipoEntrada.EVENTO).count());
        assertEquals(2, r.stream().filter(e -> e.tipo() == TipoEntrada.TRABAJO).count()); // evento Trabajo + tarea con proyecto
        assertEquals(1, r.stream().filter(e -> e.tipo() == TipoEntrada.TAREA).count());
        assertEquals(1, r.stream().filter(e -> e.tipo() == TipoEntrada.CUMPLE).count());
        assertEquals(1, r.stream().filter(e -> e.tipo() == TipoEntrada.HABITO).count());   // viernes: sin el semanal ni el inactivo
    }

    @Test void filtraPorSegmentoVisible() {
        var soloCumples = CalendarioUnificadoService.delDia(HOY, datos(), EnumSet.of(TipoEntrada.CUMPLE));
        assertEquals(1, soloCumples.size());
        assertEquals("Cumple 26 años", soloCumples.getFirst().detalle());
        assertTrue(CalendarioUnificadoService.delDia(HOY, datos(), EnumSet.noneOf(TipoEntrada.class)).isEmpty());
    }

    @Test void habitoMarcadoYSemanalSoloLosLunes() {
        var hab = CalendarioUnificadoService.delDia(HOY, datos(), EnumSet.of(TipoEntrada.HABITO));
        assertTrue(hab.getFirst().hecha());
        var lunes = CalendarioUnificadoService.delDia(LocalDate.of(2026, 10, 19), datos(), EnumSet.of(TipoEntrada.HABITO));
        assertEquals(2, lunes.size());
        assertFalse(lunes.getFirst().hecha());
    }

    @Test void cumpleSeRepiteCadaAnioYElBisiestoCaeElDia28() {
        var d = new CalendarioUnificadoService.Datos(List.of(), List.of(), List.of(),
            List.of(new Contacto(9, "Leap", "", "", "", "2000-02-29", "", false, null)), Map.of());
        assertEquals(1, CalendarioUnificadoService.delDia(LocalDate.of(2027, 2, 28), d, TODO).size());
        assertEquals(0, CalendarioUnificadoService.delDia(LocalDate.of(2027, 3, 1), d, TODO).size());
        assertEquals(1, CalendarioUnificadoService.delDia(LocalDate.of(2028, 2, 29), d, TODO).size());
    }

    @Test void ordenaTodoElDiaPrimeroYLuegoPorHora() {
        var r = CalendarioUnificadoService.delDia(HOY, datos(), TODO);
        assertTrue(r.getFirst().todoElDia());
        var conHora = r.stream().filter(e -> !e.todoElDia()).toList();
        assertEquals(List.of("Dentista", "Reunión"), conHora.stream().map(Entrada::titulo).toList());
    }

    @Test void horaCorruptaSeMuestraComoTodoElDia() {
        var d = new CalendarioUnificadoService.Datos(List.of(ev(1, "Raro", "Personal", "25:99", "x")),
            List.of(), List.of(), List.of(), Map.of());
        var r = CalendarioUnificadoService.delDia(HOY, d, TODO);
        assertEquals(1, r.size());
        assertTrue(r.getFirst().todoElDia());
    }

    @Test void carrilesRepartenEventosSolapados() {
        var d = new CalendarioUnificadoService.Datos(List.of(
            ev(1, "A", "Personal", "09:00", "11:00"), ev(2, "B", "Personal", "10:00", "12:00"),
            ev(3, "C", "Personal", "13:00", "14:00")), List.of(), List.of(), List.of(), Map.of());
        var conHora = CalendarioUnificadoService.delDia(HOY, d, TODO);
        var c = CalendarioUnificadoService.asignarCarriles(conHora);
        var a = c.stream().filter(x -> x.entrada().titulo().equals("A")).findFirst().orElseThrow();
        var b = c.stream().filter(x -> x.entrada().titulo().equals("B")).findFirst().orElseThrow();
        var cc = c.stream().filter(x -> x.entrada().titulo().equals("C")).findFirst().orElseThrow();
        assertEquals(2, a.carriles());
        assertNotEquals(a.carril(), b.carril());
        assertEquals(1, cc.carriles());   // C no se solapa con nadie
    }
}
