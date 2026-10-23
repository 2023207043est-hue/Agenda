package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Calendario;
import java.sql.*;
import java.util.*;

public class CalendarioDAO {
    public List<Calendario> listar() {
        List<Calendario> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM calendario ORDER BY id");
             ResultSet s = p.executeQuery()) {
            while (s.next()) r.add(new Calendario(s.getInt("id"), s.getString("nombre"), s.getString("color")));
        } catch (SQLException x) { com.agendapersonal.util.Log.error("Calendario SELECT: ".trim(), x); }
        return r;
    }

    public boolean insertar(Calendario k) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO calendario(nombre,color) VALUES(?,?)")) {
            p.setString(1, k.getNombre()); p.setString(2, k.getColor());
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("CalendarioDAO", x); return false; }
    }

    public boolean actualizar(Calendario k) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE calendario SET nombre=?,color=? WHERE id=?")) {
            p.setString(1, k.getNombre()); p.setString(2, k.getColor()); p.setInt(3, k.getId());
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("CalendarioDAO", x); return false; }
    }

    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM calendario WHERE id=?")) {
            p.setInt(1, id);
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.Log.error("CalendarioDAO", x); return false; }
    }
}
