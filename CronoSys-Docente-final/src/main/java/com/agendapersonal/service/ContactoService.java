package com.agendapersonal.service;

import com.agendapersonal.domain.model.Contacto;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

/** Reglas del CRM personal: último contacto, seguimiento y cumpleaños (offline, sin BD). */
public final class ContactoService {
    private ContactoService() {}

    /** Contacto que lleva tiempo sin saber de ti. {@code dias == null} = nunca contactado. */
    public record Seguimiento(Contacto contacto, Long dias) {}

    /** Cumpleaños próximo. {@code enDias == 0} significa hoy. */
    public record Cumple(Contacto contacto, LocalDate fecha, int enDias, int edad) {}

    public static void marcarContactadoHoy(Contacto c, LocalDate hoy) {
        c.setUltimoContacto(hoy.toString());
    }

    public static OptionalLong diasSinContacto(Contacto c, LocalDate hoy) {
        LocalDate f = fecha(c.getUltimoContacto());
        return f == null ? OptionalLong.empty() : OptionalLong.of(ChronoUnit.DAYS.between(f, hoy));
    }

    /** Contactos con más de {@code umbralDias} sin contacto (o nunca), los más olvidados primero. */
    public static List<Seguimiento> requierenSeguimiento(Collection<Contacto> todos, LocalDate hoy, int umbralDias) {
        List<Seguimiento> r = new ArrayList<>();
        for (Contacto c : todos) {
            OptionalLong d = diasSinContacto(c, hoy);
            if (d.isEmpty()) r.add(new Seguimiento(c, null));
            else if (d.getAsLong() > umbralDias) r.add(new Seguimiento(c, d.getAsLong()));
        }
        r.sort(Comparator.comparingLong((Seguimiento s) -> s.dias() == null ? Long.MAX_VALUE : s.dias()).reversed());
        return r;
    }

    /** Cumpleaños dentro de los próximos {@code ventanaDias} días (incluye hoy), en orden. */
    public static List<Cumple> proximosCumpleanos(Collection<Contacto> todos, LocalDate hoy, int ventanaDias) {
        List<Cumple> r = new ArrayList<>();
        for (Contacto c : todos) {
            LocalDate nac = fecha(c.getCumpleanos());
            if (nac == null) continue;
            MonthDay md = MonthDay.from(nac);
            LocalDate prox = md.atYear(hoy.getYear());
            if (prox.isBefore(hoy)) prox = md.atYear(hoy.getYear() + 1);
            int en = (int) ChronoUnit.DAYS.between(hoy, prox);
            if (en <= ventanaDias) r.add(new Cumple(c, prox, en, prox.getYear() - nac.getYear()));
        }
        r.sort(Comparator.comparingInt(Cumple::enDias));
        return r;
    }

    private static LocalDate fecha(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }
}
