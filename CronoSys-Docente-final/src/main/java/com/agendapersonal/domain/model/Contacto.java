package com.agendapersonal.domain.model;

public class Contacto {
    private int id; private String nombre; private String telefono; private String email;
    private String categoria; private String cumpleanos; private String notas;
    private boolean favorito; private String ultimoContacto;
    public Contacto() {}
    public Contacto(int id, String nombre, String telefono, String email, String categoria,
                    String cumpleanos, String notas, boolean favorito, String ultimoContacto) {
        this.id=id; this.nombre=nombre; this.telefono=telefono; this.email=email; this.categoria=categoria;
        this.cumpleanos=cumpleanos; this.notas=notas; this.favorito=favorito; this.ultimoContacto=ultimoContacto;
    }
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public String getCumpleanos(){return cumpleanos;} public void setCumpleanos(String v){cumpleanos=v;}
    public String getNotas(){return notas;} public void setNotas(String v){notas=v;}
    public boolean isFavorito(){return favorito;} public void setFavorito(boolean v){favorita(v);}
    private void favorita(boolean v){favorito=v;}
    public String getUltimoContacto(){return ultimoContacto;} public void setUltimoContacto(String v){ultimoContacto=v;}
    private String apodo, fechas;
    public String getApodo(){return apodo;} public void setApodo(String v){apodo=v;}
    public String getFechas(){return fechas;} public void setFechas(String v){fechas=v;}
    @Override public String toString(){return nombre + (categoria!=null?" • "+categoria:"");}
}
