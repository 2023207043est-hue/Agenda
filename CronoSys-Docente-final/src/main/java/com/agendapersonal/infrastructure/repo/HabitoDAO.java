package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Habito;
import java.sql.*;
import java.util.*;

public class HabitoDAO {
    public boolean insertar(Habito h) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO habito(nombre,descripcion,frecuencia,meta,color,activo) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, h.getNombre()); p.setString(2, h.getDescripcion()); p.setString(3, h.getFrecuencia());
            p.setInt(4, h.getMeta()); p.setString(5, h.getColor()); p.setInt(6, h.isActivo() ? 1 : 0);
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("HabitoDAO", e); return false; }
    }
    public List<Habito> listar() {
        List<Habito> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM habito ORDER BY activo DESC,nombre");
             ResultSet s = p.executeQuery()) {
            while (s.next()) r.add(new Habito(s.getInt("id"), s.getString("nombre"), s.getString("descripcion"),
                s.getString("frecuencia"), s.getInt("meta"), s.getString("color"), s.getInt("activo") == 1));
        } catch (Exception e) { com.agendapersonal.util.Log.error("habito: ".trim(), e); }
        return r;
    }
    public boolean actualizar(Habito h) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE habito SET nombre=?,descripcion=?,frecuencia=?,meta=?,color=?,activo=? WHERE id=?")) {
            p.setString(1, h.getNombre()); p.setString(2, h.getDescripcion()); p.setString(3, h.getFrecuencia());
            p.setInt(4, h.getMeta()); p.setString(5, h.getColor()); p.setInt(6, h.isActivo() ? 1 : 0);
            p.setInt(7, h.getId()); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("HabitoDAO", e); return false; }
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM habito WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("HabitoDAO", e); return false; }
    }
    public boolean marcar(int habitoId, String fecha) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO habito_check(habito_id,fecha,cumplido) VALUES(?,?,1) ON CONFLICT(habito_id,fecha) DO UPDATE SET cumplido=1")) {
            p.setInt(1, habitoId); p.setString(2, fecha); p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("HabitoDAO", e); return false; }
    }
    public boolean desmarcar(int habitoId, String fecha) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM habito_check WHERE habito_id=? AND fecha=?")) {
            p.setInt(1, habitoId); p.setString(2, fecha); p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("HabitoDAO", e); return false; }
    }
    public Set<Integer> cumplidos(String fecha) {
        Set<Integer> r = new HashSet<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT habito_id FROM habito_check WHERE fecha=? AND cumplido=1")) {
            p.setString(1, fecha);
            try (ResultSet s = p.executeQuery()) { while (s.next()) r.add(s.getInt(1)); }
        } catch (Exception e) { com.agendapersonal.util.Log.error("check: ".trim(), e); }
        return r;
    }
    /** Checks cumplidos entre dos fechas ISO (inclusive): fecha → ids de hábitos cumplidos ese día. */
    public Map<String, Set<Integer>> cumplidosEntre(String desde, String hasta) {
        Map<String, Set<Integer>> r = new HashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT habito_id,fecha FROM habito_check WHERE cumplido=1 AND fecha BETWEEN ? AND ?")) {
            p.setString(1, desde); p.setString(2, hasta);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.computeIfAbsent(s.getString(2), k -> new HashSet<>()).add(s.getInt(1));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("checks: ".trim(), e); }
        return r;
    }

    public int racha(int habitoId) {
        int racha = 0;
        java.time.LocalDate d = java.time.LocalDate.now();
        Set<String> fechas = new HashSet<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT fecha FROM habito_check WHERE habito_id=? AND cumplido=1")) {
            p.setInt(1, habitoId);
            try (ResultSet s = p.executeQuery()) { while (s.next()) fechas.add(s.getString(1)); }
        } catch (Exception e) { return 0; }
        // Si hoy todavía no se marcó, la racha sigue viva desde ayer (antes mostraba 0 toda la mañana).
        if (!fechas.contains(d.toString())) d = d.minusDays(1);
        while (fechas.contains(d.minusDays(racha).toString())) racha++;
        return racha;
    }
}
