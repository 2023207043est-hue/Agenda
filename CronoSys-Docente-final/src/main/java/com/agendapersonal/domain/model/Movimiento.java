package com.agendapersonal.domain.model;

public class Movimiento {
    private int id; private String tipo; private String categoria;
    private double monto; private String fecha; private String descripcion;
    public Movimiento() {}
    public Movimiento(int id, String tipo, String categoria, double monto, String fecha, String descripcion) {
        this.id=id; this.tipo=tipo; this.categoria=categoria; this.monto=monto; this.fecha=fecha; this.descripcion=descripcion;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public double getMonto(){return monto;} public void setMonto(double v){monto=v;}
    public String getFecha(){return fecha;} public void setFecha(String v){fecha=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    @Override public String toString(){return tipo + " " + monto + " • " + categoria;}
}
