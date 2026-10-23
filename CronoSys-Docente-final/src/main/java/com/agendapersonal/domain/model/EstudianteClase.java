package com.agendapersonal.domain.model;

/** Registro de estudiante dentro de una clase docente, con nota y observaciones opcionales. */
public record EstudianteClase(int id, int claseId, String nombre, String correo,
                              Double calificacion, String observaciones) {}
