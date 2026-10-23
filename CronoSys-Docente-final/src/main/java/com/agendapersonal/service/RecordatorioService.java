package com.agendapersonal.service;

import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Qué hay que recordarle al usuario y cuándo. Lógica pura (sin UI ni BD) y con pruebas.
 * Dos usos: (1) resumen al abrir el programa, (2) avisos en vivo mientras corre en segundo plano.
 */
public final class RecordatorioService {
    private RecordatorioService() {}

    /** Mensaje listo para mostrar. {@code clave} es única por aviso para no repetirlo. */
    public record Aviso(String clave, TipoEntrada tipo, String titulo, String mensaje) {}

    /** Noticias del día para la ventana de bienvenida. */
    public record Resumen(LocalDate hoy,
                          List<ContactoService.Cumple> cumples,     // hoy y próximos días (ordenados)
                          List<Evento> eventosHoy, List<Evento> eventosManana,
                          List<Tarea> tareasHoy, List<Tarea> tareasVencidas,
                          List<Habito> habitosPendientes) {
        /** Habilita la ventana emergente: cumpleaños cercano, evento hoy/mañana o tareas con fecha. */
        public boolean hayNoticias() {
            return !cumples.isEmpty() || !eventosHoy.isEmpty() || !eventosManana.isEmpty()
                || !tareasHoy.isEmpty() || !tareasVencidas.isEmpty();
        }
    }

    public static Resumen resumenInicio(LocalDate hoy, CalendarioUnificadoService.Datos d, int ventanaCumples) {
        var cumples = ContactoService.proximosCumpleanos(d.contactos(), hoy, ventanaCumples);
        List<Evento> evHoy = eventosDe(hoy, d), evMan = eventosDe(hoy.plusDays(1), d);
        List<Tarea> tHoy = new ArrayList<>(), tVenc = new ArrayList<>();
        for (Tarea t : d.tareas()) {
            if (t.estaCompletada()) continue;
            if (hoy.equals(t.getFecha())) tHoy.add(t);
            else if (t.getFecha() != null && t.getFecha().isBefore(hoy)) tVenc.add(t);
        }
        tVenc.sort(Comparator.comparing(Tarea::getFecha));
        var hechos = d.habitosCumplidos().getOrDefault(hoy.toString(), Set.of());
        List<Habito> hab = d.habitos().stream()
            .filter(h -> h.isActivo() && CalendarioUnificadoService.aplicaHabito(h, hoy) && !hechos.contains(h.getId()))
            .toList();
        return new Resumen(hoy, cumples, evHoy, evMan, tHoy, tVenc, hab);
    }

    private static List<Evento> eventosDe(LocalDate dia, CalendarioUnificadoService.Datos d) {
        return d.eventos().stream().filter(e -> dia.equals(e.getFecha()))
            .sorted(Comparator.comparing(e -> e.getHoraInicio() == null ? "" : e.getHoraInicio())).toList();
    }

    /**
     * Eventos de hoy que empiezan dentro de {@code minutosAntes} (o empezaron hace menos de 1 min)
     * y que todavía no se avisaron. No modifica {@code yaAvisados}: lo hace quien muestra el aviso.
     */
    public static List<Aviso> avisosEventos(LocalDateTime ahora, CalendarioUnificadoService.Datos d,
                                            int minutosAntes, Set<String> yaAvisados) {
        List<Aviso> r = new ArrayList<>();
        for (Evento e : d.eventos()) {
            if (!ahora.toLocalDate().equals(e.getFecha())) continue;
            LocalDateTime inicio;
            try { inicio = e.getFecha().atTime(e.getInicio()); } catch (DateTimeParseException ex) { continue; }
            long faltan = Duration.between(ahora, inicio).toMinutes();
            if (inicio.isBefore(ahora.minusMinutes(1)) || faltan > minutosAntes) continue;
            String clave = "ev:" + e.getId() + ":" + e.getFecha() + ":" + e.getHoraInicio();
            if (yaAvisados.contains(clave)) continue;
            TipoEntrada t = CalendarioUnificadoService.esTrabajo(e) ? TipoEntrada.TRABAJO : TipoEntrada.EVENTO;
            String cuando = faltan <= 0 ? "Empieza ahora" : "Empieza en " + faltan + " min";
            r.add(new Aviso(clave, t, e.getTitulo(), cuando + " · " + e.getHoraInicio() + "–" + e.getHoraFin()));
        }
        r.sort(Comparator.comparing(Aviso::clave));
        return r;
    }

    /** Cumpleaños de hoy aún no avisados (se usa cuando el programa sigue abierto al cambiar el día). */
    public static List<Aviso> avisosCumpleHoy(LocalDate hoy, CalendarioUnificadoService.Datos d, Set<String> yaAvisados) {
        List<Aviso> r = new ArrayList<>();
        for (var c : ContactoService.proximosCumpleanos(d.contactos(), hoy, 0)) {
            String clave = "cumple:" + c.contacto().getId() + ":" + hoy;
            if (yaAvisados.contains(clave)) continue;
            r.add(new Aviso(clave, TipoEntrada.CUMPLE, "Hoy cumple " + c.contacto().getNombre(),
                c.edad() > 0 ? "Cumple " + c.edad() + " años. ¡No olvides saludarle!" : "¡No olvides saludarle!"));
        }
        return r;
    }

    /** Aviso único con el resumen del día (para cuando cambia el día y el programa sigue en segundo plano). */
    public static Aviso avisoResumen(Resumen s) {
        int cumplesHoy = (int) s.cumples().stream().filter(c -> c.enDias() == 0).count();
        List<String> partes = new ArrayList<>();
        if (cumplesHoy > 0) partes.add(cumplesHoy + " cumpleaños");
        if (!s.eventosHoy().isEmpty()) partes.add(s.eventosHoy().size() + (s.eventosHoy().size() == 1 ? " evento" : " eventos"));
        int tareas = s.tareasHoy().size() + s.tareasVencidas().size();
        if (tareas > 0) partes.add(tareas + (tareas == 1 ? " tarea" : " tareas"));
        String msg = partes.isEmpty() ? "Sin pendientes para hoy." : "Hoy: " + String.join(", ", partes) + ".";
        return new Aviso("resumen:" + s.hoy(), TipoEntrada.EVENTO, "Resumen del día", msg);
    }
}
