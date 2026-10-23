package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Nota;
import java.sql.*;
import java.util.*;

public class NotaDAO {
    public boolean insertar(Nota n) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO nota(titulo,contenido,etiquetas,tipo,fecha_creacion,fecha_update,favorita) VALUES(?,?,?,?,?,?,?)")) {
            p.setString(1, n.getTitulo()); p.setString(2, n.getContenido()); p.setString(3, n.getEtiquetas());
            p.setString(4, n.getTipo()); p.setString(5, n.getFechaCreacion()); p.setString(6, n.getFechaUpdate());
            p.setInt(7, n.isFavorita() ? 1 : 0); p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("NotaDAO", e); return false; }
    }
    public List<Nota> listar(String tipo) {
        List<Nota> r = new ArrayList<>();
        String q = tipo == null ? "SELECT * FROM nota ORDER BY favorita DESC,id DESC" : "SELECT * FROM nota WHERE tipo=? ORDER BY favorita DESC,id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            if (tipo != null) p.setString(1, tipo);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new Nota(s.getInt("id"), s.getString("titulo"), s.getString("contenido"),
                    s.getString("etiquetas"), s.getString("tipo"), s.getString("fecha_creacion"),
                    s.getString("fecha_update"), s.getInt("favorita") == 1));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("nota: ".trim(), e); }
        return r;
    }
    public boolean actualizar(Nota n) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE nota SET titulo=?,contenido=?,etiquetas=?,tipo=?,fecha_update=?,favorita=? WHERE id=?")) {
            p.setString(1, n.getTitulo()); p.setString(2, n.getContenido()); p.setString(3, n.getEtiquetas());
            p.setString(4, n.getTipo()); p.setString(5, n.getFechaUpdate()); p.setInt(6, n.isFavorita() ? 1 : 0);
            p.setInt(7, n.getId()); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("NotaDAO", e); return false; }
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM nota WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("NotaDAO", e); return false; }
    }
}
