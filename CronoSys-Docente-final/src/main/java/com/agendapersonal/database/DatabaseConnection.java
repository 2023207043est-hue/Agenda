package com.agendapersonal.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

/**
 * Conexión a la base de datos de Compás.
 * <ul>
 *   <li>Ubicación fija por usuario ({@code %APPDATA%\CronoSys} en Windows, {@code ~/.cronosys} en otros),
 *       con el nombre histórico de la app para no perder los datos de quien ya la usaba
 *       y sin depender de la carpeta desde la que se abre el programa.</li>
 *   <li>Se puede forzar otra ruta con {@code -Dcronosys.db=ruta} (las pruebas usan un archivo temporal).</li>
 *   <li>Cada conexión activa las claves foráneas (SQLite las ignora por defecto) y espera si la BD está ocupada.</li>
 * </ul>
 */
public class DatabaseConnection {
    public static final String PROP_RUTA = "cronosys.db";
    private DatabaseConnection() {}

    /** Carpeta de datos del usuario (se crea si no existe). */
    public static Path dataDir() {
        Path d;
        String forzada = System.getProperty(PROP_RUTA);
        if (forzada != null && !forzada.isBlank()) {
            Path p = Paths.get(forzada).toAbsolutePath().getParent();
            d = p != null ? p : Paths.get(".").toAbsolutePath();
        } else {
            String appdata = System.getenv("APPDATA");
            d = appdata != null && !appdata.isBlank()
                ? Paths.get(appdata, "CronoSys")
                : Paths.get(System.getProperty("user.home"), ".cronosys");
        }
        try { Files.createDirectories(d); } catch (IOException ignored) { /* se reportará al abrir la BD */ }
        return d;
    }

    public static Path dbPath() {
        String forzada = System.getProperty(PROP_RUTA);
        if (forzada != null && !forzada.isBlank()) return Paths.get(forzada).toAbsolutePath();
        return dataDir().resolve("agenda_personal.db");
    }

    public static Connection getConnection() throws SQLException {
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.enforceForeignKeys(true);
        cfg.setBusyTimeout(5000);
        return cfg.createConnection("jdbc:sqlite:" + dbPath());
    }

    /** Si existe la BD antigua junto al programa y aún no hay una en la carpeta del usuario, la copia (sin borrarla). */
    static void importarBdAntigua() {
        if (System.getProperty(PROP_RUTA) != null) return;
        Path nueva = dbPath();
        Path vieja = Paths.get("agenda_personal.db").toAbsolutePath();
        if (Files.exists(nueva) || !Files.exists(vieja)) return;
        try {
            Files.copy(vieja, nueva, StandardCopyOption.COPY_ATTRIBUTES);
            com.agendapersonal.util.Log.info("Base de datos copiada desde " + vieja + " a " + nueva);
        } catch (IOException e) { com.agendapersonal.util.Log.error("No se pudo copiar la BD antigua", e); }
    }

    public static void initializeDatabase() {
        importarBdAntigua();
        com.agendapersonal.infrastructure.db.Database.migrate();
    }
}
