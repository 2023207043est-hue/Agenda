package com.agendapersonal.model;

import java.time.LocalDate;

public final class Evento {
    private int id;
    private String titulo;
    private LocalDate fecha;
    private String horaInicio;
    private String horaFin;
    private String descripcion;
    private String categoria;
    private Prioridad prioridad = Prioridad.MEDIA;
    private Integer calendarioId;

    public Evento() {}

    public Evento(int id, String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad) {
        this(id,titulo,fecha,horaInicio,horaFin,descripcion,categoria,prioridad,null);
    }

    public Evento(int id, String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad, Integer calendarioId) {
        this.id=id; this.titulo=titulo; this.fecha=fecha; this.horaInicio=horaInicio;
        this.horaFin=horaFin; this.descripcion=descripcion; this.categoria=categoria; setPrioridad(prioridad);
        this.calendarioId=calendarioId;
    }

    public Evento(String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad) {
        this(0,titulo,fecha,horaInicio,horaFin,descripcion,categoria,prioridad);
    }

    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public LocalDate getFecha(){return fecha;} public void setFecha(LocalDate v){fecha=v;}
    public String getHoraInicio(){return horaInicio;} public void setHoraInicio(String v){horaInicio=v;}
    public String getHoraFin(){return horaFin;} public void setHoraFin(String v){horaFin=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public String getPrioridad(){return prioridad.etiqueta();}
    public Prioridad getPrioridadTipo(){return prioridad;}
    public void setPrioridad(String v){prioridad=Prioridad.desde(v);}
    public void setPrioridad(Prioridad v){prioridad=java.util.Objects.requireNonNull(v);}

    /** Hora de inicio tipada (acepta "9:00" o "09:00"); lanza DateTimeParseException si es inválida. */
    public java.time.LocalTime getInicio(){return Horas.parsear(horaInicio);}
    /** Hora de fin tipada. */
    public java.time.LocalTime getFin(){return Horas.parsear(horaFin);}
    public Integer getCalendarioId(){return calendarioId;} public void setCalendarioId(Integer v){calendarioId=v;}

    @Override public String toString(){return horaInicio+" - "+titulo+" ("+fecha+")";}
}