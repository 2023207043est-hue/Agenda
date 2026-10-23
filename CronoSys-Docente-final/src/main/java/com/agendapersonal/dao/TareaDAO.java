package com.agendapersonal.dao;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.model.EstadoTarea;
import com.agendapersonal.model.Prioridad;
import com.agendapersonal.model.Tarea;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class TareaDAO {
    public boolean insertar(Tarea t) {
        String q="INSERT INTO tarea(titulo,descripcion,estado,fecha,prioridad,proyecto_id,hora,destacada,evento_id,bloque_id) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,t.getTitulo()); p.setString(2,t.getDescripcion()); p.setString(3,t.getEstado());
            p.setString(4,t.getFecha().toString()); p.setString(5,t.getPrioridad());
            if(t.getProyectoId()==null) p.setNull(6,Types.INTEGER); else p.setInt(6,t.getProyectoId());
            extras(p,t,7);
            p.executeUpdate(); return true;
        } catch (Exception x) { com.agendapersonal.util.Log.error("TareaDAO", x); return false; }
    }

    public List<Tarea> listarTodas() {
        List<Tarea> r=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM tarea ORDER BY fecha,id");
            ResultSet s=p.executeQuery()) {
            while(s.next()) r.add(map(s));
        } catch(SQLException x){com.agendapersonal.util.Log.error("Tarea SELECT: ".trim(), x);}
        return r;
    }

    public boolean actualizar(Tarea t) {
        String q="UPDATE tarea SET titulo=?,descripcion=?,estado=?,fecha=?,prioridad=?,proyecto_id=?,hora=?,destacada=?,evento_id=?,bloque_id=? WHERE id=?";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,t.getTitulo()); p.setString(2,t.getDescripcion()); p.setString(3,t.getEstado());
            p.setString(4,t.getFecha().toString()); p.setString(5,t.getPrioridad());
            if(t.getProyectoId()==null) p.setNull(6,Types.INTEGER); else p.setInt(6,t.getProyectoId());
            extras(p,t,7);
            p.setInt(11,t.getId());
            return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("TareaDAO", x); return false; }
    }

    public boolean eliminar(int id) {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM tarea WHERE id=?")) {
            p.setInt(1,id); return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("TareaDAO", x); return false; }
    }

    /** Hora, destacada y vínculos (4 parámetros desde la posición dada). */
    private static void extras(PreparedStatement p, Tarea t, int i) throws SQLException {
        if(t.getHora()==null) p.setNull(i,Types.VARCHAR); else p.setString(i,t.getHora());
        p.setInt(i+1,t.isDestacada()?1:0);
        if(t.getEventoId()==null) p.setNull(i+2,Types.INTEGER); else p.setInt(i+2,t.getEventoId());
        if(t.getBloqueId()==null) p.setNull(i+3,Types.INTEGER); else p.setInt(i+3,t.getBloqueId());
    }

    private Tarea map(ResultSet s)throws SQLException {
        int pid;
        Integer proyectoId = null;
        try { pid = s.getInt("proyecto_id"); if(!s.wasNull()) proyectoId = pid; } catch(Exception ignored) {}
        Tarea t = new Tarea(s.getInt("id"),s.getString("titulo"),s.getString("descripcion"),
                null,LocalDate.parse(s.getString("fecha")),null,proyectoId);
        t.setEstado(EstadoTarea.desdeBD(s.getString("estado")));        // tolerante con datos antiguos
        t.setPrioridad(Prioridad.desdeBD(s.getString("prioridad")));
        try {
            t.setHora(s.getString("hora"));
            t.setDestacada(s.getInt("destacada")==1);
            int e=s.getInt("evento_id"); if(!s.wasNull()) t.setEventoId(e);
            int b=s.getInt("bloque_id"); if(!s.wasNull()) t.setBloqueId(b);
        } catch(SQLException ignored) { /* base antigua sin las columnas nuevas */ }
        return t;
    }
}