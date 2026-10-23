package com.agendapersonal.service;

import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Horas;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio de la agenda (sin UI ni base de datos, 100 % testeable):
 * validación, duración, detección de solapamientos y sugerencia de huecos libres.
 */
public final class AgendaService {
    private AgendaService() {}

    /** Intervalo semiabierto [inicio, fin) en minutos desde las 00:00. */
    private record Tramo(int ini, int fin) {}

    // ---------- Validación ----------

    /** @return mensaje de error, o vacío si el evento es válido. */
    public static Optional<String> validar(Evento e) {
        if (e.getTitulo() == null || e.getTitulo().isBlank()) return Optional.of("El título es obligatorio.");
        if (e.getFecha() == null) return Optional.of("La fecha es obligatoria.");
        try {
            LocalTime ini = e.getInicio(), fin = e.getFin();
            if (!fin.isAfter(ini)) return Optional.of("La hora de fin debe ser posterior a la de inicio.");
        } catch (DateTimeParseException ex) {
            return Optional.of("Use el formato de hora HH:mm (ej. 09:30).");
        }
        return Optional.empty();
    }

    // ---------- Duración ----------

    /** Duración exacta del evento en minutos. Requiere un evento válido. */
    public static int duracionMinutos(Evento e) {
        return (int) Duration.between(e.getInicio(), e.getFin()).toMinutes();
    }

    /** 90 → "1 h 30 min", 45 → "45 min", 120 → "2 h". */
    public static String formatearMinutos(int minutos) {
        int h = minutos / 60, m = minutos % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    // ---------- Solapamientos ----------

    /**
     * Calcula la intersección real de rangos (no solo "misma hora de inicio").
     * Eventos que solo se tocan en el borde (10:00 fin / 10:00 inicio) NO se cruzan.
     * Ignora el propio evento (mismo id ≠ 0) para permitir editarlo.
     */
    public static List<ConflictoHorario> detectarConflictos(Evento nuevo, Collection<Evento> existentes) {
        List<ConflictoHorario> r = new ArrayList<>();
        Tramo t = tramoDe(nuevo);
        if (t == null) return r;
        for (Evento o : existentes) {
            if (o.getFecha() == null || !o.getFecha().equals(nuevo.getFecha())) continue;
            if (nuevo.getId() != 0 && o.getId() == nuevo.getId()) continue;
            Tramo u = tramoDe(o);
            if (u == null) continue;
            int solape = Math.min(t.fin, u.fin) - Math.max(t.ini, u.ini);
            if (solape > 0) r.add(new ConflictoHorario(o, solape));
        }
        r.sort(Comparator.comparingInt((ConflictoHorario c) -> c.existente().getInicio().toSecondOfDay()));
        return r;
    }

    // ---------- Carga y huecos libres ----------

    /** Minutos realmente ocupados en un día (une eventos solapados para no contar dos veces). */
    public static int minutosOcupados(LocalDate dia, Collection<Evento> eventos) {
        int total = 0;
        for (Tramo t : unir(tramosDelDia(dia, eventos))) total += t.fin - t.ini;
        return total;
    }

    /**
     * Sugiere el primer hueco libre de al menos {@code duracionMin} minutos dentro de la
     * ventana [desde, hasta] del día indicado.
     */
    public static Optional<LocalTime[]> sugerirHueco(LocalDate dia, Collection<Evento> eventos,
                                                      int duracionMin, LocalTime desde, LocalTime hasta) {
        int cursor = desde.toSecondOfDay() / 60, limite = hasta.toSecondOfDay() / 60;
        for (Tramo t : unir(tramosDelDia(dia, eventos))) {
            if (t.fin <= cursor) continue;
            if (t.ini - cursor >= duracionMin) break;
            cursor = Math.max(cursor, t.fin);
        }
        if (cursor + duracionMin > limite) return Optional.empty();
        return Optional.of(new LocalTime[]{LocalTime.ofSecondOfDay(cursor * 60L),
                                           LocalTime.ofSecondOfDay((cursor + duracionMin) * 60L)});
    }

    // ---------- Internos ----------

    private static Tramo tramoDe(Evento e) {
        try {
            int i = e.getInicio().toSecondOfDay() / 60, f = e.getFin().toSecondOfDay() / 60;
            return f > i ? new Tramo(i, f) : null;
        } catch (DateTimeParseException ex) { return null; }
    }

    private static List<Tramo> tramosDelDia(LocalDate dia, Collection<Evento> eventos) {
        List<Tramo> r = new ArrayList<>();
        for (Evento e : eventos) {
            if (!dia.equals(e.getFecha())) continue;
            Tramo t = tramoDe(e);
            if (t != null) r.add(t);
        }
        return r;
    }

    private static List<Tramo> unir(List<Tramo> in) {
        List<Tramo> s = new ArrayList<>(in);
        s.sort(Comparator.comparingInt(Tramo::ini));
        List<Tramo> out = new ArrayList<>();
        for (Tramo t : s) {
            if (!out.isEmpty() && t.ini <= out.getLast().fin) {
                Tramo l = out.removeLast();
                out.add(new Tramo(l.ini, Math.max(l.fin, t.fin)));
            } else out.add(t);
        }
        return out;
    }

    /** Formato "HH:mm" para mostrar horas calculadas. */
    public static String hora(LocalTime t) { return t.format(Horas.SALIDA); }
}
