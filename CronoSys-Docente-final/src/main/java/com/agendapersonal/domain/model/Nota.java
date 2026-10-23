package com.agendapersonal.domain.model;

public class Nota {
    private int id; private String titulo; private String contenido; private String etiquetas;
    private String tipo; private String fechaCreacion; private String fechaUpdate; private boolean favorita;
    public Nota() {}
    public Nota(int id, String titulo, String contenido, String etiquetas, String tipo,
                String fechaCreacion, String fechaUpdate, boolean favorita) {
        this.id=id; this.titulo=titulo; this.contenido=contenido; this.etiquetas=etiquetas;
        this.tipo=tipo; this.fechaCreacion=fechaCreacion; this.fechaUpdate=fechaUpdate; this.favorita=favorita;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getContenido(){return contenido;} public void setContenido(String v){contenido=v;}
    public String getEtiquetas(){return etiquetas;} public void setEtiquetas(String v){etiquetas=v;}
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
    public String getFechaCreacion(){return fechaCreacion;} public void setFechaCreacion(String v){fechaCreacion=v;}
    public String getFechaUpdate(){return fechaUpdate;} public void setFechaUpdate(String v){fechaUpdate=v;}
    public boolean isFavorita(){return favorita;} public void setFavorita(boolean v){favorita=v;}
    @Override public String toString(){return (tipo!=null&&tipo.equals("IDEA")?"Idea · ":"")+titulo;}
}
