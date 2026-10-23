package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Almacén central de ajustes: JSON versionado con perfiles. No depende de JavaFX ni de la base de
 * datos (usa un {@link Storage}); cualquier valor ausente o inválido cae al valor por defecto.
 * Los datos del usuario (eventos, tareas, notas...) viven en otras tablas y nunca se tocan aquí.
 */
public final class SettingsStore {
    public static final int VERSION = 1;
    public static final String KEY = "settings.json";
    /** Marcador de los archivos exportados. Se conserva el nombre antiguo para que los archivos
     *  exportados antes del cambio de marca sigan importándose. */
    public static final String FILE_TYPE = "cronosys-settings";

    public interface Storage { String get(String key, String def); void set(String key, String value); }

    private static SettingsStore instance;

    /** Instancia global (se conecta a la tabla config la primera vez). */
    public static synchronized SettingsStore get() {
        if (instance == null) instance = new SettingsStore(new ConfigStorage());
        return instance;
    }

    /** Para pruebas. */
    public static synchronized void install(SettingsStore s) { instance = s; }

    private final Storage storage;
    private Map<String, Object> root;
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public SettingsStore(Storage storage) {
        this.storage = storage;
        this.root = load();
    }

    // ------------------------------------------------------------------ carga y migración

    private Map<String, Object> load() {
        String raw = null;
        try { raw = storage.get(KEY, null); } catch (RuntimeException ignored) { }
        if (raw != null && !raw.isBlank()) {
            try {
                Map<String, Object> r = sanitize(Json.parseObject(raw));
                if (r != null) return r;
            } catch (RuntimeException ignored) { /* ajustes corruptos: se rehacen desde los antiguos */ }
        }
        Map<String, Object> r = migrateLegacy();
        try { storage.set(KEY, Json.write(r)); } catch (RuntimeException ignored) { }
        return r;
    }

    /** Crea ajustes nuevos tomando lo que ya existía en las claves antiguas (ui.theme, ui.accent...). */
    private Map<String, Object> migrateLegacy() {
        Map<String, Object> s = Defaults.settings();
        Map<String, Object> ap = section(s, "appearance");
        String theme = safe("ui.theme", "Claro");
        boolean dark = "Oscuro".equalsIgnoreCase(theme);
        ap.put("themeId", dark ? "oscuro" : "claro");
        ap.put("mode", dark ? "oscuro" : "claro");
        ap.put("accentId", safe("ui.accent", "NARANJA").toUpperCase(Locale.ROOT));
        String font = safe("ui.font", "");
        if (!font.isBlank()) ap.put("font", font);
        ap.put("fontScale", switch (safe("ui.fontSize", "NORMAL")) { case "PEQUENO" -> 89; case "GRANDE" -> 111; case "MUY_GRANDE" -> 121; default -> 100; });
        section(s, "sidebar").put("collapsed", "1".equals(safe("ui.sidebar.collapsed", "0")));
        Map<String, Object> home = section(s, "home");
        home.put("hour24", !"0".equals(safe("reloj.h24", "1")));
        home.put("showSeconds", "1".equals(safe("reloj.segundos", "0")));
        home.put("userName", safe("usuario.nombre", ""));
        Map<String, Object> noti = section(s, "notifications");
        noti.put("confirmations", "1".equals(safe("ui.confirmaciones", "0")));

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", "default");
        p.put("name", "Principal");
        p.put("schedule", null);
        p.put("settings", s);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("version", VERSION);
        r.put("activeProfileId", "default");
        r.put("profiles", new ArrayList<>(List.of(p)));
        return r;
    }

    private String safe(String k, String def) {
        try { String v = storage.get(k, def); return v == null ? def : v; } catch (RuntimeException e) { return def; }
    }

    /** Valida la estructura (no los valores: esos se corrigen al leerlos). null si no es utilizable. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitize(Map<String, Object> r) {
        Object v = r.get("version");
        if (!(v instanceof Number n) || n.intValue() < 1 || n.intValue() > VERSION) return null;
        if (!(r.get("profiles") instanceof List<?> raw)) return null;
        List<Object> profiles = new ArrayList<>();
        for (Object o : raw) {
            if (!(o instanceof Map<?, ?> pm)) continue;
            Map<String, Object> p = (Map<String, Object>) pm;
            if (!(p.get("id") instanceof String id) || id.isBlank()) continue;
            if (!(p.get("name") instanceof String nm) || nm.isBlank()) p.put("name", "Perfil");
            if (!(p.get("settings") instanceof Map)) p.put("settings", new LinkedHashMap<String, Object>());
            if (!validSchedule(p.get("schedule"))) p.put("schedule", null);
            profiles.add(p);
        }
        if (profiles.isEmpty()) return null;
        r.put("profiles", profiles);
        r.put("version", VERSION);
        String active = r.get("activeProfileId") instanceof String a ? a : "";
        boolean ok = false;
        for (Object o : profiles) if (((Map<?, ?>) o).get("id").equals(active)) ok = true;
        if (!ok) r.put("activeProfileId", ((Map<?, ?>) profiles.get(0)).get("id"));
        return r;
    }

    private static boolean validTime(Object o) { return o instanceof String s && s.matches("([01]?\\d|2[0-3]):[0-5]\\d"); }

    /** Un horario es válido si tiene al menos una franja con horas correctas. */
    private static boolean validSchedule(Object o) {
        if (!(o instanceof Map<?, ?> sc)) return false;
        if (sc.get("windows") instanceof List<?> ws) {
            for (Object w : ws)
                if (w instanceof Map<?, ?> wm && validTime(wm.get("from")) && validTime(wm.get("to"))) return true;
            return false;
        }
        return validTime(sc.get("from")) && validTime(sc.get("to"));
    }

    // ------------------------------------------------------------------ lectura tipada

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> settings, String name) {
        return (Map<String, Object>) settings.get(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> activeSettings() {
        Map<String, Object> p = profile(activeProfileId());
        return (Map<String, Object>) p.get("settings");
    }

    /** Valor crudo de "seccion.clave" (admite anidados "health.goals.water"); null si no existe en el perfil. */
    public Object raw(String path) { return dig(activeSettings(), path); }

    private static Object dig(Map<String, Object> m, String path) {
        Object cur = m;
        for (String part : path.split("\\.")) {
            if (!(cur instanceof Map<?, ?> mm)) return null;
            cur = mm.get(part);
        }
        return cur;
    }

    private static final Map<String, Object> DEFAULTS = Defaults.settings();

    public Object value(String path) {
        Object v = raw(path);
        return v != null ? v : dig(DEFAULTS, path);
    }

    public String str(String path) {
        Object v = raw(path), d = dig(DEFAULTS, path);
        if (v instanceof String s) return s;
        return d instanceof String ds ? ds : "";
    }

    public boolean bool(String path) {
        Object v = raw(path), d = dig(DEFAULTS, path);
        if (v instanceof Boolean b) return b;
        return d instanceof Boolean db && db;
    }

    /** Entero acotado a [min, max]; si falta o es inválido, el valor por defecto (también acotado). */
    public int integer(String path, int min, int max) {
        Object v = raw(path), d = dig(DEFAULTS, path);
        double x;
        if (v instanceof Number n && !Double.isNaN(n.doubleValue())) x = n.doubleValue();
        else x = d instanceof Number dn ? dn.doubleValue() : min;
        return (int) Math.max(min, Math.min(max, Math.round(x)));
    }

    /** Texto restringido a opciones válidas. */
    public String choice(String path, String... allowed) {
        String v = str(path);
        for (String a : allowed) if (a.equals(v)) return v;
        Object d = dig(DEFAULTS, path);
        return d instanceof String ds ? ds : allowed[0];
    }

    public List<String> strings(String path) {
        Object v = raw(path);
        if (!(v instanceof List<?> l)) v = dig(DEFAULTS, path);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l2) for (Object o : l2) if (o instanceof String s) out.add(s);
        return out;
    }

    public Map<String, Object> map(String path) {
        Object v = raw(path);
        Map<String, Object> out = new LinkedHashMap<>();
        if (v instanceof Map<?, ?> m) for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
        return out;
    }

    // ------------------------------------------------------------------ escritura

    /** Guarda un valor en "seccion.clave" del perfil activo, persiste y avisa a los oyentes. */
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = activeSettings();
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cur.get(parts[i]);
            if (!(next instanceof Map)) { next = new LinkedHashMap<String, Object>(); cur.put(parts[i], next); }
            cur = (Map<String, Object>) next;
        }
        if (value == null) cur.remove(parts[parts.length - 1]); else cur.put(parts[parts.length - 1], value);
        persist();
        fire(parts[0]);
    }

    /** Restablece una sección completa del perfil activo a sus valores por defecto. */
    public void resetSection(String section) {
        activeSettings().remove(section);
        persist();
        fire(section);
    }

    public void addListener(Consumer<String> l) { listeners.add(l); }
    public void removeListener(Consumer<String> l) { listeners.remove(l); }

    private void fire(String section) { for (Consumer<String> l : listeners) { try { l.accept(section); } catch (RuntimeException ignored) { } } }

    private void persist() {
        try { storage.set(KEY, Json.write(root)); } catch (RuntimeException ignored) { }
    }

    // ------------------------------------------------------------------ perfiles

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> profiles() { return (List<Map<String, Object>>) (List<?>) root.get("profiles"); }

    public String activeProfileId() { return (String) root.get("activeProfileId"); }

    public Map<String, Object> profile(String id) {
        for (Map<String, Object> p : profiles()) if (p.get("id").equals(id)) return p;
        return profiles().get(0);
    }

    public String activeProfileName() { return (String) profile(activeProfileId()).get("name"); }

    public void activateProfile(String id) {
        boolean found = false;
        for (Map<String, Object> p : profiles()) if (p.get("id").equals(id)) found = true;
        if (!found || id.equals(activeProfileId())) return;
        root.put("activeProfileId", id);
        persist();
        fire("*");
    }

    public String createProfile(String name) {
        Map<String, Object> p = newProfile(name, Defaults.settings());
        profiles().add(p);
        persist();
        return (String) p.get("id");
    }

    @SuppressWarnings("unchecked")
    public String duplicateProfile(String id) {
        Map<String, Object> src = profile(id);
        Map<String, Object> copy = (Map<String, Object>) Json.parseObject(Json.write(src.get("settings")));
        Map<String, Object> p = newProfile(src.get("name") + " (copia)", copy);
        profiles().add(p);
        persist();
        return (String) p.get("id");
    }

    public void renameProfile(String id, String name) {
        if (name == null || name.isBlank()) return;
        profile(id).put("name", name.trim());
        persist();
    }

    /** Borra un perfil; siempre queda al menos uno. Devuelve false si no se pudo. */
    public boolean deleteProfile(String id) {
        if (profiles().size() <= 1) return false;
        boolean removed = profiles().removeIf(p -> p.get("id").equals(id));
        if (!removed) return false;
        boolean wasActive = id.equals(activeProfileId());
        if (wasActive) root.put("activeProfileId", profiles().get(0).get("id"));
        persist();
        if (wasActive) fire("*");
        return true;
    }

    public void setSchedule(String id, String from, String to) {
        Map<String, Object> p = profile(id);
        if (validTime(from) && validTime(to)) {
            Map<String, Object> sc = new LinkedHashMap<>();
            sc.put("from", from); sc.put("to", to);
            p.put("schedule", sc);
        } else p.put("schedule", null);
        persist();
    }

    /** Una franja horaria ("08:00" a "14:00"). Si la hora de fin es menor, la franja cruza la medianoche. */
    public record Franja(String desde, String hasta) {
        public int desdeMinutos() { return SettingsStore.minutes(desde); }
        public int hastaMinutos() { return SettingsStore.minutes(hasta); }
        /** ¿La franja cubre ese minuto del día? */
        public boolean contiene(int minuto) {
            int a = desdeMinutos(), b = hastaMinutos();
            if (a == b) return false;
            return a <= b ? (minuto >= a && minuto < b) : (minuto >= a || minuto < b);
        }
    }

    /** Todas las franjas horarias de un perfil (admite el formato antiguo de una sola franja). */
    public List<Franja> franjas(String id) {
        List<Franja> out = new ArrayList<>();
        Object sc = profile(id).get("schedule");
        if (!(sc instanceof Map<?, ?> m)) return out;
        if (m.get("windows") instanceof List<?> ws) {
            for (Object w : ws)
                if (w instanceof Map<?, ?> wm && validTime(wm.get("from")) && validTime(wm.get("to")))
                    out.add(new Franja(String.valueOf(wm.get("from")), String.valueOf(wm.get("to"))));
        } else if (validTime(m.get("from")) && validTime(m.get("to"))) {
            out.add(new Franja(String.valueOf(m.get("from")), String.valueOf(m.get("to"))));
        }
        return out;
    }

    /** Días de la semana del horario (1 = lunes … 7 = domingo). Vacío = todos los días. */
    public List<Integer> dias(String id) {
        List<Integer> out = new ArrayList<>();
        Object sc = profile(id).get("schedule");
        if (sc instanceof Map<?, ?> m && m.get("days") instanceof List<?> ds) {
            for (Object d : ds)
                if (d instanceof Number n && n.intValue() >= 1 && n.intValue() <= 7 && !out.contains(n.intValue()))
                    out.add(n.intValue());
        }
        return out;
    }

    /** Guarda varias franjas y los días de la semana en los que se activan. */
    public void setScheduleWindows(String id, List<Franja> franjas, List<Integer> dias) {
        Map<String, Object> p = profile(id);
        List<Object> ventanas = new ArrayList<>();
        for (Franja f : franjas) {
            if (!validTime(f.desde()) || !validTime(f.hasta())) continue;
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("from", f.desde()); w.put("to", f.hasta());
            ventanas.add(w);
        }
        if (ventanas.isEmpty()) p.put("schedule", null);
        else {
            Map<String, Object> sc = new LinkedHashMap<>();
            sc.put("windows", ventanas);
            sc.put("days", dias == null ? new ArrayList<>() : new ArrayList<>(dias));
            p.put("schedule", sc);
        }
        persist();
    }

    /** Activa el perfil cuyo horario incluya la hora dada (minutos desde 00:00), si hay alguno. */
    public boolean applySchedule(int minuteOfDay) { return applySchedule(minuteOfDay, -1); }

    /**
     * Activa el perfil cuyo horario incluya la hora y el día dados.
     * {@code diaSemana} de 1 (lunes) a 7 (domingo); si es negativo se ignoran los días.
     */
    public boolean applySchedule(int minuteOfDay, int diaSemana) {
        for (Map<String, Object> p : profiles()) {
            String id = String.valueOf(p.get("id"));
            List<Franja> franjas = franjas(id);
            if (franjas.isEmpty()) continue;
            List<Integer> dias = dias(id);
            if (diaSemana > 0 && !dias.isEmpty() && !dias.contains(diaSemana)) continue;
            for (Franja f : franjas) {
                if (!f.contiene(minuteOfDay)) continue;
                if (id.equals(activeProfileId())) return false;
                activateProfile(id);
                return true;
            }
        }
        return false;
    }

    public static int minutes(String hhmm) {
        if (!validTime(hhmm)) return 0;
        String[] t = hhmm.split(":");
        return Integer.parseInt(t[0]) * 60 + Integer.parseInt(t[1]);
    }

    private static Map<String, Object> newProfile(String name, Map<String, Object> settings) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", UUID.randomUUID().toString().substring(0, 8));
        p.put("name", name == null || name.isBlank() ? "Perfil" : name.trim());
        p.put("schedule", null);
        p.put("settings", settings);
        return p;
    }

    // ------------------------------------------------------------------ exportar / importar

    /** Exporta el perfil activo o todos a JSON. */
    public String export(boolean all) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", FILE_TYPE);
        out.put("version", VERSION);
        List<Object> ps = new ArrayList<>();
        if (all) ps.addAll(profiles()); else ps.add(profile(activeProfileId()));
        out.put("profiles", ps);
        out.put("activeProfileId", all ? activeProfileId() : profile(activeProfileId()).get("id"));
        return Json.pretty(out);
    }

    public record ImportPreview(boolean valid, String message, List<String> profileNames, Map<String, Object> parsed) {}

    /** Valida un archivo sin modificar nada. */
    public static ImportPreview previewImport(String text) {
        Map<String, Object> r;
        try { r = Json.parseObject(text); }
        catch (RuntimeException e) { return new ImportPreview(false, "El archivo no es un JSON válido.", List.of(), null); }
        if (!FILE_TYPE.equals(r.get("type"))) return new ImportPreview(false, "El archivo no es una exportación de Compás.", List.of(), null);
        if (!(r.get("version") instanceof Number n)) return new ImportPreview(false, "El archivo no tiene versión.", List.of(), null);
        if (n.intValue() > VERSION) return new ImportPreview(false, "El archivo es de una versión más nueva (" + n.intValue() + "). Actualiza Compás.", List.of(), null);
        if (n.intValue() < 1) return new ImportPreview(false, "Versión de archivo inválida.", List.of(), null);
        Map<String, Object> clean;
        try { clean = sanitize(r); } catch (RuntimeException e) { clean = null; }
        if (clean == null) return new ImportPreview(false, "El archivo no contiene perfiles válidos.", List.of(), null);
        List<String> names = new ArrayList<>();
        for (Object o : (List<?>) clean.get("profiles")) names.add((String) ((Map<?, ?>) o).get("name"));
        return new ImportPreview(true, "Se añadirán " + names.size() + " perfil(es): " + String.join(", ", names) + ".", names, clean);
    }

    /** Aplica una importación ya validada: añade los perfiles (con ids nuevos) y activa el primero. */
    @SuppressWarnings("unchecked")
    public void applyImport(ImportPreview pv) {
        if (pv == null || !pv.valid()) return;
        String first = null;
        for (Object o : (List<?>) pv.parsed().get("profiles")) {
            Map<String, Object> p = (Map<String, Object>) o;
            Map<String, Object> np = newProfile((String) p.get("name"), (Map<String, Object>) p.get("settings"));
            np.put("schedule", p.get("schedule"));
            profiles().add(np);
            if (first == null) first = (String) np.get("id");
        }
        persist();
        if (first != null) { root.put("activeProfileId", first); persist(); fire("*"); }
    }

    /** Restablece todos los ajustes del perfil activo. */
    public void resetActiveProfile() {
        profile(activeProfileId()).put("settings", Defaults.settings());
        persist();
        fire("*");
    }

    /** Reinicia todos los perfiles y preferencias tras borrar todos los datos de la aplicación. */
    public void resetAll() {
        root = load();
        fire("*");
    }
}
