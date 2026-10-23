package com.agendapersonal.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PerfilTest {
    @Test void saludaSegunLaHoraYUsaElPrimerNombre() {
        assertEquals("Buenos días, Jason", Perfil.saludo(8, "  jason   selmouni "));
        assertEquals("Buenas tardes, Ana", Perfil.saludo(15, "Ana"));
        assertEquals("Buenas noches, Luz", Perfil.saludo(22, "luz"));
    }

    @Test void sinNombreSoloSaluda() {
        assertEquals("Buenos días", Perfil.saludo(7, ""));
        assertEquals("Buenas noches", Perfil.saludo(21, null));
        assertEquals("Buenas tardes", Perfil.saludo(12, "   "));
    }

    @Test void limpiaEspaciosYLimitaElLargo() {
        assertEquals("Ana María", Perfil.limpiar("  Ana    María "));
        assertEquals(40, Perfil.limpiar("x".repeat(100)).length());
        assertEquals("", Perfil.limpiar(null));
    }
}
