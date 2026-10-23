package com.agendapersonal.domain.model;

public class RegistroSalud {
    private int id; private String tipo; private double valor; private String unidad;
    private String fecha; private String notas;
    public RegistroSalud() {}
    public RegistroSalud(int id, String tipo, double valor, String unidad, String fecha, String notas) {
        this.id=id; this.tipo=tipo; this.valor=valor; this.unidad=unidad; this.fecha=fecha; this.notas=notas;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public double getValor(){return valor;} public void setValor(double v){valor=v;}
    public String getUnidad(){return unidad;} public void setUnidad(String v){unidad=v;}
    public String getFecha(){return fecha;} public void setFecha(String v){fecha=v;}
    public String getNotas(){return notas;} public void setNotas(String v){notas=v;}
    @Override public String toString(){return tipo + ": " + valor + (unidad!=null?" "+unidad:"");}
}
