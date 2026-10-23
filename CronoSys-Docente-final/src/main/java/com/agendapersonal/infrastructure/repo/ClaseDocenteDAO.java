package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.domain.model.ActividadClase;
import com.agendapersonal.domain.model.ClaseDocente;
import com.agendapersonal.domain.model.EstudianteClase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a cursos, listas de clase, notas y actividades docentes. */
public final class ClaseDocenteDAO {
    public List<ClaseDocente> listarClases() {
        List<ClaseDocente> salida = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id,nombre,grupo,periodo FROM clase_docente ORDER BY nombre,grupo");
             ResultSet r = p.executeQuery()) {
            while (r.next()) salida.add(new ClaseDocente(r.getInt("id"), r.getString("nombre"),
                r.getString("grupo"), r.getString("periodo")));
        } catch (Exception e) { com.agendapersonal.util.Log.error("listar clases docentes", e); }
        return salida;
    }

    public int crearClase(String nombre, String grupo, String periodo) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "INSERT INTO clase_docente(nombre,grupo,periodo) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, nombre.trim()); p.setString(2, limpio(grupo)); p.setString(3, limpio(periodo));
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { return r.next() ? r.getInt(1) : 0; }
        } catch (Exception e) { com.agendapersonal.util.Log.error("crear clase docente", e); return 0; }
    }

    public boolean eliminarClase(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM clase_docente WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("eliminar clase docente", e); return false; }
    }

    public List<EstudianteClase> listarEstudiantes(int claseId) {
        List<EstudianteClase> salida = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT id,clase_id,nombre,correo,calificacion,observaciones
                 FROM estudiante_clase WHERE clase_id=? ORDER BY nombre COLLATE NOCASE
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    double nota = r.getDouble("calificacion");
                    Double nullableNota = r.wasNull() ? null : nota;
                    salida.add(new EstudianteClase(r.getInt("id"), r.getInt("clase_id"), r.getString("nombre"),
                        r.getString("correo"), nullableNota, r.getString("observaciones")));
                }
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("listar estudiantes de clase", e); }
        return salida;
    }

    /** Inserta o actualiza por nombre dentro del curso, evitando duplicados al reimportar el Excel. */
    public boolean guardarEstudiante(int claseId, String nombre, String correo, Double calificacion, String observaciones) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("""
                 INSERT INTO estudiante_clase(clase_id,nombre,correo,calificacion,observaciones)
                 VALUES(?,?,?,?,?)
                 ON CONFLICT(clase_id,nombre) DO UPDATE SET
                    correo=excluded.correo, calificacion=excluded.calificacion, observaciones=excluded.observaciones
                 """)) {
            p.setInt(1, claseId); p.setString(2, nombre.trim()); p.setString(3, limpio(correo));
            if (calificacion == null) p.setNull(4, java.sql.Types.REAL); else p.setDouble(4, calificacion);
            p.setString(5, limpio(observaciones));
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("guardar estudiante de clase", e); return false; }
    }

    public boolean eliminarEstudiante(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM estudiante_clase WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("eliminar estudiante de clase", e); return false; }
    }

    public List<ActividadClase> listarActividades(int claseId) {
        List<ActividadClase> salida = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT id,clase_id,titulo,tipo,fecha,descripcion FROM actividad_clase
                 WHERE clase_id=? ORDER BY CASE WHEN fecha IS NULL THEN 1 ELSE 0 END,fecha,titulo
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String fecha = r.getString("fecha");
                    salida.add(new ActividadClase(r.getInt("id"), r.getInt("clase_id"), r.getString("titulo"),
                        r.getString("tipo"), fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha),
                        r.getString("descripcion")));
                }
            }
        } catch (Exception e) { com.agendapersonal.util.Log.error("listar actividades de clase", e); }
        return salida;
    }

    public boolean crearActividad(int claseId, String titulo, String tipo, LocalDate fecha, String descripcion) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO actividad_clase(clase_id,titulo,tipo,fecha,descripcion) VALUES(?,?,?,?,?)")) {
            p.setInt(1, claseId); p.setString(2, titulo.trim()); p.setString(3, tipo);
            p.setString(4, fecha == null ? null : fecha.toString()); p.setString(5, limpio(descripcion));
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("crear actividad de clase", e); return false; }
    }

    public boolean eliminarActividad(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM actividad_clase WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.Log.error("eliminar actividad de clase", e); return false; }
    }

    private static String limpio(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
}
