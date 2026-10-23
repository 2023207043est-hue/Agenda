package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.Contacto;
import java.sql.*;
import java.util.*;

public class ContactoDAO {
    public boolean insertar(Contacto x) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO contacto(nombre,telefono,email,categoria,cumpleanos,notas,favorito,ultimo_contacto,apodo,fechas) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1, x.getNombre()); p.setString(2, x.getTelefono()); p.setString(3, x.getEmail());
            p.setString(4, x.getCategoria()); p.setString(5, x.getCumpleanos()); p.setString(6, x.getNotas());
            p.setInt(7, x.isFavorito() ? 1 : 0); p.setString(8, x.getUltimoContacto());
            p.setString(9, x.getApodo()); p.setString(10, x.getFechas());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ContactoDAO", e); return false; }
    }
    public List<Contacto> listar() {
        List<Contacto> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM contacto ORDER BY favorito DESC,nombre");
             ResultSet s = p.executeQuery()) {
            while (s.next()) {
                Contacto x = new Contacto(s.getInt("id"), s.getString("nombre"), s.getString("telefono"),
                    s.getString("email"), s.getString("categoria"), s.getString("cumpleanos"), s.getString("notas"),
                    s.getInt("favorito") == 1, s.getString("ultimo_contacto"));
                x.setApodo(s.getString("apodo"));
                x.setFechas(s.getString("fechas"));
                r.add(x);
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("contacto: ".trim(), e); }
        return r;
    }
    public boolean actualizar(Contacto x) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE contacto SET nombre=?,telefono=?,email=?,categoria=?,cumpleanos=?,notas=?,favorito=?,ultimo_contacto=?,apodo=?,fechas=? WHERE id=?")) {
            p.setString(1, x.getNombre()); p.setString(2, x.getTelefono()); p.setString(3, x.getEmail());
            p.setString(4, x.getCategoria()); p.setString(5, x.getCumpleanos()); p.setString(6, x.getNotas());
            p.setInt(7, x.isFavorito() ? 1 : 0); p.setString(8, x.getUltimoContacto());
            p.setString(9, x.getApodo()); p.setString(10, x.getFechas());
            p.setInt(11, x.getId()); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ContactoDAO", e); return false; }
    }
    public boolean eliminar(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM contacto WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("ContactoDAO", e); return false; }
    }
}
