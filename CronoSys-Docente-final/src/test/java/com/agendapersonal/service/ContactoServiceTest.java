package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.Contacto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static Contacto c(String nombre, String ultimo, String cumple) {
        return new Contacto(1, nombre, "", "", "Amigos", cumple, "", false, ultimo);
    }

    @Test void marcarContactadoHoy() {
        Contacto x = c("Ana", null, null);
        ContactoService.marcarContactadoHoy(x, HOY);
        assertEquals("2026-10-16", x.getUltimoContacto());
        assertEquals(0, ContactoService.diasSinContacto(x, HOY).getAsLong());
    }

    @Test void seguimientoOrdenaPorMasOlvidado() {
        var lista = List.of(c("Reciente", "2026-10-10", null), c("Viejo", "2026-06-01", null),
                            c("Medio", "2026-09-01", null), c("Nunca", null, null));
        var r = ContactoService.requierenSeguimiento(lista, HOY, 30);
        assertEquals(List.of("Nunca", "Viejo", "Medio"), r.stream().map(s -> s.contacto().getNombre()).toList());
    }

    @Test void cumpleanosProximosIncluyenCambioDeAnio() {
        var lista = List.of(c("Hoy", null, "2000-10-16"), c("En5", null, "1999-10-21"),
                            c("FinDeAnio", null, "1990-12-31"), c("EneroSig", null, "1995-01-02"),
                            c("Lejano", null, "1990-05-05"));
        var r = ContactoService.proximosCumpleanos(lista, HOY, 90);
        assertEquals(List.of("Hoy", "En5", "FinDeAnio", "EneroSig"), r.stream().map(k -> k.contacto().getNombre()).toList());
        assertEquals(0, r.getFirst().enDias());
        assertEquals(26, r.getFirst().edad());
        assertEquals(32, r.get(3).edad());   // cumple el 2-ene-2027: 2027 - 1995
    }

    @Test void fechasInvalidasSeIgnoran() {
        assertTrue(ContactoService.proximosCumpleanos(List.of(c("X", null, "no-es-fecha")), HOY, 365).isEmpty());
    }
}
