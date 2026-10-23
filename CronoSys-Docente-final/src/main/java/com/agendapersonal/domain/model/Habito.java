package com.agendapersonal.domain.model;

public class Habito {
    private int id; private String nombre; private String descripcion;
    private String frecuencia; private int meta; private String color; private boolean activo;
    public Habito() {}
    public Habito(int id, String nombre, String descripcion, String frecuencia, int meta, String color, boolean activo) {
        this.id=id; this.nombre=nombre; this.descripcion=descripcion; this.frecuencia=frecuencia;
        this.meta=meta; this.color=color; this.activo=activo;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getFrecuencia(){return frecuencia;} public void setFrecuencia(String v){frecuencia=v;}
    public int getMeta(){return meta;} public void setMeta(int v){meta=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}
    public boolean isActivo(){return activo;} public void setActivo(boolean v){activo=v;}
    @Override public String toString(){return nombre + " • " + frecuencia;}
}
