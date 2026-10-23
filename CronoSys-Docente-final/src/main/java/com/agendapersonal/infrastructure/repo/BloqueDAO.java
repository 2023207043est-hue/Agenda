package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.BloqueTiempo;
import java.sql.*;
import java.util.*;

public class BloqueDAO {
    public boolean insertar(BloqueTiempo b) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,categoria,evento_id) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, b.getTitulo()); p.setString(2, b.getFecha()); p.setString(3, com.agendapersonal.model.Horas.normalizar(b.getHoraInicio()));
            p.setString(4, com.agendapersonal.model.Horas.normalizar(b.getHoraFin())); p.setString(5, b.getCategoria());
            if (b.getEventoId() == null) p.setNull(6, Types.INTEGER); else p.setInt(6, b.getEventoId());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("BloqueDAO", e); return false; }
    }
    public List<BloqueTiempo> porFecha(String fecha) {
        List<BloqueTiempo> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha=? ORDER BY hora_inicio")) {
            p.setString(1, fecha);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new BloqueTiempo(s.getInt("id"), s.getString("titulo"), s.getString("fecha"),
                    s.getString("hora_inicio"), s.getString("hora_fin"), s.getString("categoria"),
                    s.getObject("evento_id") == null ? null : s.getInt("evento_id")));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("bloque: ".trim(), e); }
        return r;
    }
    /** Bloques de hoy en adelante (para vincularlos a una tarea). */
    public List<BloqueTiempo> proximos() {
        List<BloqueTiempo> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha>=? ORDER BY fecha, hora_inicio LIMIT 200")) {
            p.setString(1, java.time.LocalDate.now().toString());
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new BloqueTiempo(s.getInt("id"), s.getString("titulo"), s.getString("fecha"),
                    s.getString("hora_inicio"), s.getString("hora_fin"), s.getString("categoria"),
                    s.getObject("evento_id") == null ? null : s.getInt("evento_id")));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("bloque proximos", e); }
        return r;
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM bloque_tiempo WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("BloqueDAO", e); return false; }
    }
}
