package com.agendapersonal.dao;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Horas;
import com.agendapersonal.model.Prioridad;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class EventoDAO {
    public boolean insertar(Evento e) {
        String q="INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,descripcion,categoria,prioridad,calendario_id) VALUES(?,?,?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,e.getTitulo()); p.setString(2,e.getFecha().toString()); p.setString(3,Horas.normalizar(e.getHoraInicio()));
            p.setString(4,Horas.normalizar(e.getHoraFin())); p.setString(5,e.getDescripcion()); p.setString(6,e.getCategoria()); p.setString(7,e.getPrioridad());
            if(e.getCalendarioId()==null) p.setNull(8,Types.INTEGER); else p.setInt(8,e.getCalendarioId());
            p.executeUpdate(); return true;
        } catch(SQLException x){com.agendapersonal.util.Log.error("Evento INSERT: ".trim(), x); return false;}
    }

    public List<Evento> listarTodos() {
        List<Evento> r=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM evento ORDER BY fecha,hora_inicio");
            ResultSet s=p.executeQuery()) {
            while(s.next()) r.add(map(s));
        } catch(SQLException x){com.agendapersonal.util.Log.error("Evento SELECT: ".trim(), x);}
        return r;
    }

    public List<Evento> listarPorFecha(LocalDate f) {
        List<Evento> r=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM evento WHERE fecha=? ORDER BY hora_inicio")) {
            p.setString(1,f.toString());
            try(ResultSet s=p.executeQuery()){while(s.next()) r.add(map(s));}
        } catch(SQLException x){com.agendapersonal.util.Log.error("Evento fecha: ".trim(), x);}
        return r;
    }

    public boolean actualizar(Evento e) {
        String q="UPDATE evento SET titulo=?,fecha=?,hora_inicio=?,hora_fin=?,descripcion=?,categoria=?,prioridad=?,calendario_id=? WHERE id=?";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,e.getTitulo()); p.setString(2,e.getFecha().toString()); p.setString(3,Horas.normalizar(e.getHoraInicio()));
            p.setString(4,Horas.normalizar(e.getHoraFin())); p.setString(5,e.getDescripcion()); p.setString(6,e.getCategoria()); p.setString(7,e.getPrioridad());
            if(e.getCalendarioId()==null) p.setNull(8,Types.INTEGER); else p.setInt(8,e.getCalendarioId());
            p.setInt(9,e.getId());
            return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("EventoDAO", x); return false; }
    }

    public boolean eliminar(int id) {
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement b=c.prepareStatement("UPDATE bloque_tiempo SET evento_id=NULL WHERE evento_id=?");
                PreparedStatement p=c.prepareStatement("DELETE FROM evento WHERE id=?")) {
                b.setInt(1,id); b.executeUpdate();
                p.setInt(1,id); boolean ok=p.executeUpdate()>0;
                c.commit(); return ok;
            } catch(SQLException x){ c.rollback(); throw x; }
        } catch(SQLException x){ com.agendapersonal.util.Log.error("EventoDAO.eliminar",x); return false; }
    }

    private Evento map(ResultSet s)throws SQLException {
        Integer cal = s.getInt("calendario_id");
        if(s.wasNull()) cal = null;
        Evento ev = new Evento(s.getInt("id"),s.getString("titulo"),LocalDate.parse(s.getString("fecha")),
                s.getString("hora_inicio"),s.getString("hora_fin"),s.getString("descripcion"),
                s.getString("categoria"),null,cal);
        ev.setPrioridad(Prioridad.desdeBD(s.getString("prioridad")));  // tolerante con datos antiguos
        return ev;
    }
}