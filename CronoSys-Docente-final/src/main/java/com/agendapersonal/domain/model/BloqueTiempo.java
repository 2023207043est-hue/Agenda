package com.agendapersonal.domain.model;

public class BloqueTiempo {
    private int id; private String titulo; private String fecha;
    private String horaInicio; private String horaFin; private String categoria; private Integer eventoId;
    public BloqueTiempo() {}
    public BloqueTiempo(int id, String titulo, String fecha, String horaInicio, String horaFin, String categoria, Integer eventoId) {
        this.id=id; this.titulo=titulo; this.fecha=fecha; this.horaInicio=horaInicio;
        this.horaFin=horaFin; this.categoria=categoria; this.eventoId=eventoId;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getFecha(){return fecha;} public void setFecha(String v){fecha=v;}
    public String getHoraInicio(){return horaInicio;} public void setHoraInicio(String v){horaInicio=v;}
    public String getHoraFin(){return horaFin;} public void setHoraFin(String v){horaFin=v;}
    public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public Integer getEventoId(){return eventoId;} public void setEventoId(Integer v){eventoId=v;}
    @Override public String toString(){return horaInicio + "-" + horaFin + " " + titulo;}
}
