package com.agendapersonal.domain.model;

public class Calendario {
    private int id;
    private String nombre;
    private String color;

    public Calendario() {}

    public Calendario(int id, String nombre, String color) {
        this.id = id; this.nombre = nombre; this.color = color;
    }

    public int getId(){return id;} public void setId(int v){id=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}

    @Override public String toString(){return nombre;}
}
