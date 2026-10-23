package com.agendapersonal.infrastructure.db;

import com.agendapersonal.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class ConfigDAO {
    public Map<String, String> todas() {
        Map<String, String> m = new HashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT clave,valor FROM config");
             ResultSet s = p.executeQuery()) {
            while (s.next()) m.put(s.getString(1), s.getString(2));
        } catch (Exception e) { com.agendapersonal.util.Log.error("config: ".trim(), e); }
        return m;
    }
    public String get(String clave, String def) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT valor FROM config WHERE clave=?")) {
            p.setString(1, clave);
            try (ResultSet s = p.executeQuery()) { if (s.next()) return s.getString(1); }
        } catch (Exception e) { com.agendapersonal.util.Log.error("config get: ".trim(), e); }
        return def;
    }
    public void set(String clave, String valor) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO config(clave,valor) VALUES(?,?) ON CONFLICT(clave) DO UPDATE SET valor=excluded.valor")) {
            p.setString(1, clave); p.setString(2, valor); p.executeUpdate();
        } catch (Exception e) { com.agendapersonal.util.Log.error("config set: ".trim(), e); }
    }
}
