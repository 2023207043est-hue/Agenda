package com.agendapersonal.domain.model;

public class Proyecto {
    private int id; private String nombre; private String descripcion;
    private String estado; private String fechaInicio; private String fechaFin; private String color;
    public Proyecto() {}
    public Proyecto(int id, String nombre, String descripcion, String estado, String fechaInicio, String fechaFin, String color) {
        this.id=id; this.nombre=nombre; this.descripcion=descripcion; this.estado=estado;
        this.fechaInicio=fechaInicio; this.fechaFin=fechaFin; this.color=color;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getFechaInicio(){return fechaInicio;} public void setFechaInicio(String v){fechaInicio=v;}
    public String getFechaFin(){return fechaFin;} public void setFechaFin(String v){fechaFin=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}
    @Override public String toString(){return nombre + " • " + estado;}
}
