package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.dao.EventoDAO;
import com.agendapersonal.domain.model.Movimiento;
import com.agendapersonal.infrastructure.db.Database;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import com.agendapersonal.infrastructure.repo.MovimientoDAO;
import com.agendapersonal.model.Evento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de integración con una BD temporal (nunca toca la BD real del usuario). */
class BaseDeDatosTest {

    private static Path nuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-test");
        Path db = dir.resolve("prueba.db");
        System.setProperty(DatabaseConnection.PROP_RUTA, db.toString());
        return db;
    }

    private static int contar(String sql) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    @Test void migraEsquemaNuevoYEsIdempotente() throws Exception {
        nuevaBd();
        Database.migrate();
        assertEquals(Database.versionObjetivo(), Database.version());
        Database.migrate();                      // segunda vez: no cambia nada ni falla
        assertEquals(Database.versionObjetivo(), Database.version());
    }

    @Test void clavesForaneasActivasYBorradoEnCascada() throws Exception {
        nuevaBd(); Database.migrate();
        HabitoDAO h = new HabitoDAO();
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO habito(nombre) VALUES('Leer')");
            s.execute("INSERT INTO habito_check(habito_id,fecha) VALUES(1,'2026-10-19')");
        }
        assertTrue(h.eliminar(1));
        assertEquals(0, contar("SELECT count(*) FROM habito_check"));   // antes quedaban huérfanos
    }

    @Test void rechazaReferenciaInexistente() throws Exception {
        nuevaBd(); Database.migrate();
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            assertThrows(java.sql.SQLException.class,
                () -> s.execute("INSERT INTO tarea(titulo,estado,fecha,prioridad,proyecto_id) VALUES('x','Pendiente','2026-10-19','Media',999)"));
        }
    }

    @Test void normalizaHorasAlGuardarYOrdenaBien() throws Exception {
        nuevaBd(); Database.migrate();
        EventoDAO dao = new EventoDAO();
        LocalDate d = LocalDate.of(2026, 10, 19);
        assertTrue(dao.insertar(new Evento("Tarde", d, "10:00", "11:00", "", "Personal", "Media")));
        assertTrue(dao.insertar(new Evento("Mañana", d, "9:00", "9:30", "", "Personal", "Media")));
        var l = dao.listarPorFecha(d);
        assertEquals("Mañana", l.get(0).getTitulo());      // "9:00" antes de "10:00"
        assertEquals("09:00", l.get(0).getHoraInicio());
    }

    @Test void migracionNormalizaHorasViejas() throws Exception {
        Path db = nuevaBd();
        // BD "vieja" creada por la versión anterior: sin user_version y con horas sin cero
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE evento(id INTEGER PRIMARY KEY AUTOINCREMENT,titulo TEXT NOT NULL,fecha TEXT NOT NULL,hora_inicio TEXT NOT NULL,hora_fin TEXT NOT NULL,descripcion TEXT,categoria TEXT NOT NULL,prioridad TEXT NOT NULL)");
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('a','2026-10-19','9:00','9:30','x','Media')");
        }
        Database.migrate();
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT hora_inicio FROM evento")) {
            r.next(); assertEquals("09:00", r.getString(1));
        }
        assertEquals(Database.versionObjetivo(), Database.version());
    }

    @Test void eliminarEventoDesvinculaBloques() throws Exception {
        nuevaBd(); Database.migrate();
        try (Connection c = DatabaseConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('e','2026-10-19','09:00','10:00','x','Media')");
            s.execute("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,evento_id) VALUES('b','2026-10-19','09:00','10:00',1)");
        }
        assertTrue(new EventoDAO().eliminar(1));
        assertEquals(1, contar("SELECT count(*) FROM bloque_tiempo WHERE evento_id IS NULL"));
    }

    @Test void dineroSeGuardaEnCentavosSinErrorDeRedondeo() throws Exception {
        nuevaBd(); Database.migrate();
        MovimientoDAO dao = new MovimientoDAO();
        for (int i = 0; i < 10; i++) assertTrue(dao.insertar(new Movimiento(0, "GASTO", "Comida", 0.1, "2026-10-19", "")));
        assertEquals(100, contar("SELECT SUM(monto_centavos) FROM movimiento"));   // 10 × 10 céntimos
        assertEquals(1.0, dao.balanceMes("2026-10")[1], 1e-9);
        assertEquals(0.1, dao.listarMes("2026-10").get(0).getMonto(), 1e-9);
    }
}
