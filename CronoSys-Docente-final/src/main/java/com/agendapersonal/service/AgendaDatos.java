package com.agendapersonal.service;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.infrastructure.repo.ContactoDAO;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import java.time.LocalDate;
import java.util.List;

/** Reúne en una {@link CalendarioUnificadoService.Datos} todo lo que usan el calendario y los avisos. */
public final class AgendaDatos {
    private AgendaDatos() {}

    /** Llamar desde el hilo de JavaFX (las listas de eventos y tareas son observables). */
    public static CalendarioUnificadoService.Datos cargar(MainController c, LocalDate desde, LocalDate hasta) {
        return cargar(List.copyOf(c.getEventos()), List.copyOf(c.getTareas()), desde, hasta);
    }

    /**
     * Versión sin hilo de JavaFX: recibe copias de eventos y tareas y consulta solo hábitos y contactos,
     * así puede ejecutarse en un hilo de fondo sin congelar la ventana.
     */
    public static CalendarioUnificadoService.Datos cargar(java.util.Collection<com.agendapersonal.model.Evento> eventos,
                                                          java.util.Collection<com.agendapersonal.model.Tarea> tareas,
                                                          LocalDate desde, LocalDate hasta) {
        HabitoDAO habitos = new HabitoDAO();
        return new CalendarioUnificadoService.Datos(eventos, tareas,
            habitos.listar(), new ContactoDAO().listar(),
            habitos.cumplidosEntre(desde.toString(), hasta.toString()));
    }
}
