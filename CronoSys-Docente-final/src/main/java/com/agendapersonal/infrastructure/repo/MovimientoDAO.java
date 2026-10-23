package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Movimiento;
import java.sql.*;
import java.util.*;

public class MovimientoDAO {
    /** Importe → céntimos enteros (sin errores de coma flotante). */
    static long centavos(double monto) { return Math.round(monto * 100.0); }

    public boolean insertar(Movimiento m) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO movimiento(tipo,categoria,monto,monto_centavos,fecha,descripcion) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, m.getTipo()); p.setString(2, m.getCategoria()); p.setDouble(3, m.getMonto()); p.setLong(4, centavos(m.getMonto()));
            p.setString(5, m.getFecha()); p.setString(6, m.getDescripcion());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("MovimientoDAO", e); return false; }
    }
    public List<Movimiento> listar(int limite) {
        List<Movimiento> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM movimiento ORDER BY fecha DESC,id DESC LIMIT ?")) {
            p.setInt(1, limite);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new Movimiento(s.getInt("id"), s.getString("tipo"), s.getString("categoria"),
                    s.getLong("monto_centavos") / 100.0, s.getString("fecha"), s.getString("descripcion")));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("mov: ".trim(), e); }
        return r;
    }
    /** Movimientos de un mes (formato "2026-10"), del más reciente al más antiguo. */
    public List<Movimiento> listarMes(String yyyyMM) {
        List<Movimiento> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM movimiento WHERE substr(fecha,1,7)=? ORDER BY fecha DESC,id DESC")) {
            p.setString(1, yyyyMM);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new Movimiento(s.getInt("id"), s.getString("tipo"), s.getString("categoria"),
                    s.getLong("monto_centavos") / 100.0, s.getString("fecha"), s.getString("descripcion")));
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("mov mes: ".trim(), e); }
        return r;
    }
    public double[] balanceMes(String yyyyMM) {
        double ing = 0, gas = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT tipo,SUM(monto_centavos) FROM movimiento WHERE substr(fecha,1,7)=? GROUP BY tipo")) {
            p.setString(1, yyyyMM);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) {
                    if ("INGRESO".equals(s.getString(1))) ing = s.getLong(2) / 100.0; else gas = s.getLong(2) / 100.0;
                }
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("balance: ".trim(), e); }
        return new double[]{ing, gas, ing - gas};
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM movimiento WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("MovimientoDAO", e); return false; }
    }
}
