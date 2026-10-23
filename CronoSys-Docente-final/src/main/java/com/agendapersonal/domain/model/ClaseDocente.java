package com.agendapersonal.domain.model;

/** Una clase o grupo gestionado en el espacio docente. */
public record ClaseDocente(int id, String nombre, String grupo, String periodo) {
    @Override public String toString() {
        return grupo == null || grupo.isBlank() ? nombre : nombre + " · " + grupo;
    }
}
