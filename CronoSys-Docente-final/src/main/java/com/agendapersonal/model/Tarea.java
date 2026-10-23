package com.agendapersonal.model;

import java.time.LocalDate;

public final class Tarea {
    private int id;
    private String titulo;
    private String descripcion;
    private EstadoTarea estado = EstadoTarea.PENDIENTE;
    private LocalDate fecha;
    private Prioridad prioridad = Prioridad.MEDIA;
    private Integer proyectoId;

    public Tarea() {}

    public Tarea(int id, String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad) {
        this(id,titulo,descripcion,estado,fecha,prioridad,null);
    }

    public Tarea(int id, String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad, Integer proyectoId) {
        this.id=id; this.titulo=titulo; this.descripcion=descripcion;
        setEstado(estado); this.fecha=fecha; setPrioridad(prioridad); this.proyectoId=proyectoId;
    }

    public Tarea(String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad) {
        this(0,titulo,descripcion,estado,fecha,prioridad);
    }

    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getEstado(){return estado.etiqueta();}
    public EstadoTarea getEstadoTipo(){return estado;}
    /** Valida contra {@link EstadoTarea}; lanza IllegalArgumentException si no es válido. */
    public void setEstado(String v){estado=EstadoTarea.desde(v);}
    public void setEstado(EstadoTarea v){estado=java.util.Objects.requireNonNull(v);}
    public boolean estaCompletada(){return estado.esCompletada();}
    public LocalDate getFecha(){return fecha;} public void setFecha(LocalDate v){fecha=v;}
    public String getPrioridad(){return prioridad.etiqueta();}
    public Prioridad getPrioridadTipo(){return prioridad;}
    public void setPrioridad(String v){prioridad=Prioridad.desde(v);}
    public void setPrioridad(Prioridad v){prioridad=java.util.Objects.requireNonNull(v);}
    public Integer getProyectoId(){return proyectoId;} public void setProyectoId(Integer v){proyectoId=v;}

    // Hora opcional ("HH:mm" o null), destacada y vínculos con un evento o un bloque de time blocking.
    private String hora;
    private boolean destacada;
    private Integer eventoId, bloqueId;
    public String getHora(){return hora;}
    public void setHora(String v){hora = (v == null || v.isBlank()) ? null : v.trim();}
    public boolean isDestacada(){return destacada;} public void setDestacada(boolean v){destacada=v;}
    public Integer getEventoId(){return eventoId;} public void setEventoId(Integer v){eventoId=v;}
    public Integer getBloqueId(){return bloqueId;} public void setBloqueId(Integer v){bloqueId=v;}

    @Override public String toString(){return titulo+" • "+estado.etiqueta();}
}