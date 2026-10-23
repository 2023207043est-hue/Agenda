package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.model.EstadoTarea;
import com.agendapersonal.model.Tarea;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListaPendientesTest {

    private static Nota nota(int id, String titulo, String contenido, String etiquetas) {
        return new Nota(id, titulo, contenido, etiquetas, "NOTA", "2026-10-20", "2026-10-20", false);
    }

    @Test void sacaLasCasillasDeLasNotas() {
        List<Nota> notas = List.of(
            nota(1, "Lista del día", "- [ ] Escribir el informe\n- [x] Revisar el correo\nSin casilla", ""));
        List<ListaPendientes.Item> items = ListaPendientes.desdeNotas(notas);
        assertEquals(2, items.size());
        assertEquals("Escribir el informe", items.get(0).texto());
        assertFalse(items.get(0).hecho());
        assertEquals(0, items.get(0).linea());
        assertEquals("Revisar el correo", items.get(1).texto());
        assertTrue(items.get(1).hecho());
    }

    @Test void aceptaLosDistintosFormatosDeCasilla() {
        List<Nota> notas = List.of(nota(1, "Varios", "[ ] uno\n* [ ] dos\n☐ tres\n- [x] cuatro", ""));
        List<ListaPendientes.Item> items = ListaPendientes.desdeNotas(notas);
        assertEquals(4, items.size());
        assertEquals("uno", items.get(0).texto());
        assertEquals("dos", items.get(1).texto());
        assertEquals("tres", items.get(2).texto());
        assertEquals("cuatro", items.get(3).texto());
        assertTrue(items.get(3).hecho());
    }

    @Test void unaNotaConEtiquetaPendienteCuentaComoUnPendiente() {
        List<Nota> notas = List.of(nota(7, "Llamar al banco", "sin casillas", "pendiente, casa"));
        List<ListaPendientes.Item> items = ListaPendientes.desdeNotas(notas);
        assertEquals(1, items.size());
        assertEquals("Llamar al banco", items.get(0).texto());
        assertEquals(-1, items.get(0).linea());
    }

    @Test void marcarDesmarcaGuardaElCambioEnElTexto() {
        Nota n = nota(1, "Lista", "- [ ] Primero\n- [ ] Segundo", "");
        List<ListaPendientes.Item> items = ListaPendientes.desdeNotas(List.of(n));
        String marcado = ListaPendientes.alternar(n, items.get(0), true);
        assertTrue(marcado.startsWith("- [x] Primero"), "no se marcó: " + marcado);
        String desmarque = ListaPendientes.alternar(n, items.get(1), false);
        assertTrue(desmarque.contains("- [ ] Segundo"));
    }

    @Test void lasTareasCompletadasNoAparecen() {
        Tarea pendiente = new Tarea(1, "Hacer la compra", "", "Pendiente", LocalDate.now(), "Media");
        Tarea hecha = new Tarea(2, "Otra cosa", "", "Completada", LocalDate.now(), "Media");
        hecha.setEstado(EstadoTarea.COMPLETADA);
        List<ListaPendientes.Item> items = ListaPendientes.desdeTareas(List.of(pendiente, hecha));
        assertEquals(1, items.size());
        assertEquals("Hacer la compra", items.get(0).texto());
        assertEquals("tarea", items.get(0).origen());
    }

    @Test void noSeInventaNadaSiNoHayCasillas() {
        assertTrue(ListaPendientes.desdeNotas(List.of(nota(1, "Nota normal", "solo texto", ""))).isEmpty());
        assertTrue(ListaPendientes.desdeNotas(null).isEmpty());
        assertTrue(ListaPendientes.desdeTareas(null).isEmpty());
    }
}
