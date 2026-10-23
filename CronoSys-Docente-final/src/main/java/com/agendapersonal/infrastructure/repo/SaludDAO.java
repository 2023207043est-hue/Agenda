package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.RegistroSalud;
import java.sql.*;
import java.util.*;

public class SaludDAO {
    public boolean insertar(RegistroSalud r) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO registro_salud(tipo,valor,unidad,fecha,notas) VALUES(?,?,?,?,?)")) {
            p.setString(1, r.getTipo()); p.setDouble(2, r.getValor()); p.setString(3, r.getUnidad());
            p.setString(4, r.getFecha()); p.setString(5, r.getNotas());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("SaludDAO", e); return false; }
    }
    public List<RegistroSalud> listar(String tipo, int limite) {
        List<RegistroSalud> r = new ArrayList<>();
        String q = tipo == null ? "SELECT * FROM registro_salud ORDER BY fecha DESC,id DESC LIMIT ?"
            : "SELECT * FROM registro_salud WHERE tipo=? ORDER BY fecha DESC,id DESC LIMIT ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            if (tipo == null) p.setInt(1, limite); else { p.setString(1, tipo); p.setInt(2, limite); }
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new RegistroSalud(s.getInt("id"), s.getString("tipo"), s.getDouble("valor"),
                    s.getString("unidad"), s.getString("fecha"), s.getString("notas")));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("salud: ".trim(), e); }
        return r;
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM registro_salud WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("SaludDAO", e); return false; }
    }
}
