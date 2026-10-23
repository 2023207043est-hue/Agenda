package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Proyecto;
import java.sql.*;
import java.util.*;

public class ProyectoDAO {
    public boolean insertar(Proyecto p) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO proyecto(nombre,descripcion,estado,fecha_inicio,fecha_fin,color) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, p.getNombre()); ps.setString(2, p.getDescripcion()); ps.setString(3, p.getEstado());
            ps.setString(4, p.getFechaInicio()); ps.setString(5, p.getFechaFin()); ps.setString(6, p.getColor());
            ps.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ProyectoDAO", e); return false; }
    }
    public List<Proyecto> listar() {
        List<Proyecto> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM proyecto ORDER BY id DESC");
             ResultSet s = p.executeQuery()) {
            while (s.next()) r.add(new Proyecto(s.getInt("id"), s.getString("nombre"), s.getString("descripcion"),
                s.getString("estado"), s.getString("fecha_inicio"), s.getString("fecha_fin"), s.getString("color")));
        } catch (Exception e) { com.agendapersonal.util.Log.error("proyecto: ".trim(), e); }
        return r;
    }
    public boolean actualizar(Proyecto p) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE proyecto SET nombre=?,descripcion=?,estado=?,fecha_inicio=?,fecha_fin=?,color=? WHERE id=?")) {
            ps.setString(1, p.getNombre()); ps.setString(2, p.getDescripcion()); ps.setString(3, p.getEstado());
            ps.setString(4, p.getFechaInicio()); ps.setString(5, p.getFechaFin()); ps.setString(6, p.getColor());
            ps.setInt(7, p.getId()); return ps.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ProyectoDAO", e); return false; }
    }
    /** Elimina el proyecto y deja sus tareas sin proyecto (antes quedaban apuntando a un id inexistente). */
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement suelta = c.prepareStatement("UPDATE tarea SET proyecto_id=NULL WHERE proyecto_id=?");
             PreparedStatement p = c.prepareStatement("DELETE FROM proyecto WHERE id=?")) {
            suelta.setInt(1, id); suelta.executeUpdate();
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ProyectoDAO", e); return false; }
    }
}
