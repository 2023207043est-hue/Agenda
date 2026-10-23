package com.agendapersonal.service;

import com.agendapersonal.model.Evento;

/** Cruce detectado entre un evento propuesto y uno ya existente. */
public record ConflictoHorario(Evento existente, int minutosSolapados) {
    public String descripcion() {
        return "«" + existente.getTitulo() + "» (" + existente.getHoraInicio() + "–" + existente.getHoraFin()
            + "): se cruzan " + AgendaService.formatearMinutos(minutosSolapados);
    }
}
