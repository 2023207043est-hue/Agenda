package com.agendapersonal.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsStoreTest {
    static class Mem implements SettingsStore.Storage {
        final Map<String, String> m = new HashMap<>();
        public String get(String k, String d) { return m.getOrDefault(k, d); }
        public void set(String k, String v) { m.put(k, v); }
    }

    @Test void migraDesdeClavesAntiguas() {
        Mem mem = new Mem();
        mem.m.put("ui.theme", "Oscuro"); mem.m.put("ui.accent", "AZUL"); mem.m.put("ui.fontSize", "GRANDE");
        mem.m.put("ui.sidebar.collapsed", "1"); mem.m.put("usuario.nombre", "Ana");
        SettingsStore s = new SettingsStore(mem);
        assertEquals("oscuro", s.str("appearance.themeId"));
        assertEquals("AZUL", s.str("appearance.accentId"));
        assertEquals(111, s.integer("appearance.fontScale", 85, 125));
        assertTrue(s.bool("sidebar.collapsed"));
        assertEquals("Ana", s.str("home.userName"));
        assertNotNull(mem.m.get(SettingsStore.KEY));
    }

    @Test void valoresPorDefectoEquivalenALoDeHoy() {
        SettingsStore s = new SettingsStore(new Mem());
        assertEquals("claro", s.str("appearance.themeId"));
        assertEquals("NARANJA", s.str("appearance.accentId"));
        assertEquals(100, s.integer("appearance.fontScale", 85, 125));
        assertEquals(12, s.integer("appearance.radius", 0, 28));
    }

    @Test void ajustesCorruptosNoFallan() {
        Mem mem = new Mem();
        mem.m.put(SettingsStore.KEY, "{esto no es json");
        assertEquals("claro", new SettingsStore(mem).str("appearance.themeId"));
        mem.m.put(SettingsStore.KEY, "{\"version\":99,\"profiles\":[]}");
        assertEquals("claro", new SettingsStore(mem).str("appearance.themeId"));
        mem.m.put(SettingsStore.KEY, "{\"version\":1,\"activeProfileId\":\"x\",\"profiles\":[{\"id\":\"a\",\"name\":\"A\",\"settings\":{\"appearance\":{\"radius\":\"muchisimo\",\"fontScale\":9999,\"themeId\":7,\"density\":\"rara\"}}}]}");
        SettingsStore s = new SettingsStore(mem);
        assertEquals("a", s.activeProfileId());
        assertEquals(12, s.integer("appearance.radius", 0, 28));
        assertEquals(125, s.integer("appearance.fontScale", 85, 125));
        assertEquals("claro", s.str("appearance.themeId"));
        assertEquals("comoda", s.choice("appearance.density", "compacta", "comoda", "amplia"));
    }

    @Test void escrituraPersisteYAvisa() {
        Mem mem = new Mem();
        SettingsStore s = new SettingsStore(mem);
        int[] avisos = {0};
        s.addListener(sec -> avisos[0]++);
        s.set("appearance.radius", 20);
        s.set("health.goals.water", 10);
        assertEquals(2, avisos[0]);
        SettingsStore otra = new SettingsStore(mem);
        assertEquals(20, otra.integer("appearance.radius", 0, 28));
        assertEquals(10, otra.integer("health.goals.water", 0, 50));
        s.resetSection("appearance");
        assertEquals(12, s.integer("appearance.radius", 0, 28));
    }

    @Test void perfilesSiempreQuedaUno() {
        SettingsStore s = new SettingsStore(new Mem());
        assertFalse(s.deleteProfile(s.activeProfileId()));
        String b = s.createProfile("Estudio");
        s.set("appearance.radius", 4);
        s.activateProfile(b);
        assertEquals(12, s.integer("appearance.radius", 0, 28));   // perfil nuevo = valores por defecto
        String c = s.duplicateProfile(b);
        assertEquals("Estudio (copia)", s.profile(c).get("name"));
        assertTrue(s.deleteProfile(b));
        assertEquals("default", s.activeProfileId());
    }

    @Test void horarioDePerfilesCruzandoMedianoche() {
        SettingsStore s = new SettingsStore(new Mem());
        String noche = s.createProfile("Noche");
        s.setSchedule(noche, "19:00", "08:00");
        assertTrue(s.applySchedule(23 * 60));
        assertEquals(noche, s.activeProfileId());
        assertFalse(s.applySchedule(23 * 60 + 30));
        s.setSchedule(noche, "25:00", "08:00");
        assertNull(s.profile(noche).get("schedule"));
    }

    @Test void variasFranjasEnElHorarioDeUnPerfil() {
        SettingsStore s = new SettingsStore(new Mem());
        String estudio = s.createProfile("Estudio");
        s.setScheduleWindows(estudio, List.of(
            new SettingsStore.Franja("08:00", "12:00"),
            new SettingsStore.Franja("16:00", "18:00")), List.of(1, 2, 3, 4, 5));
        assertEquals(2, s.franjas(estudio).size());
        assertEquals(List.of(1, 2, 3, 4, 5), s.dias(estudio));

        // Lunes (1) a las 09:00 entra el perfil.
        assertTrue(s.applySchedule(9 * 60, 1));
        assertEquals(estudio, s.activeProfileId());
        // Lunes a las 13:00 no hay franja: no cambia.
        s.activateProfile(s.profiles().get(0).get("id").toString());
        assertFalse(s.applySchedule(13 * 60, 1));
        // Sábado (6) no está entre los días marcados.
        assertFalse(s.applySchedule(9 * 60, 6));
        // Sábado sí, si no se marcan días.
        s.setScheduleWindows(estudio, List.of(new SettingsStore.Franja("08:00", "12:00")), List.of());
        assertTrue(s.applySchedule(9 * 60, 6));
    }

    @Test void lasFranjasCruzanLaMedianoche() {
        SettingsStore s = new SettingsStore(new Mem());
        String noche = s.createProfile("Noche");
        s.setScheduleWindows(noche, List.of(new SettingsStore.Franja("19:00", "08:00")), List.of());
        SettingsStore.Franja f = s.franjas(noche).get(0);
        assertTrue(f.contiene(23 * 60));
        assertTrue(f.contiene(2 * 60));
        assertFalse(f.contiene(12 * 60));
        // applySchedule devuelve true solo cuando cambia el perfil activo.
        assertTrue(s.applySchedule(23 * 60, 3));
        assertEquals(noche, s.activeProfileId());
        assertFalse(s.applySchedule(2 * 60, 3));   // ya estaba activo: no cambia nada
    }

    @Test void elHorarioAntiguoSeSigueLeyendo() {
        SettingsStore s = new SettingsStore(new Mem());
        String p = s.createProfile("Antiguo");
        s.setSchedule(p, "09:00", "13:00");
        assertEquals(1, s.franjas(p).size());
        assertEquals("09:00", s.franjas(p).get(0).desde());
        assertTrue(s.applySchedule(10 * 60, 1));
    }

    @Test void exportarEImportar() {
        SettingsStore a = new SettingsStore(new Mem());
        a.set("appearance.themeId", "neon");
        a.set("appearance.colors.accent", "#FF00AA");
        String json = a.export(false);
        SettingsStore b = new SettingsStore(new Mem());
        SettingsStore.ImportPreview pv = SettingsStore.previewImport(json);
        assertTrue(pv.valid());
        b.applyImport(pv);
        assertEquals("neon", b.str("appearance.themeId"));
        assertEquals("#FF00AA", b.str("appearance.colors.accent"));
        assertEquals(2, b.profiles().size());
    }

    @Test void importacionInvalidaNoCambiaNada() {
        SettingsStore s = new SettingsStore(new Mem());
        for (String malo : new String[]{"hola", "{}", "{\"type\":\"cronosys-settings\",\"version\":5,\"profiles\":[]}", "{\"type\":\"cronosys-settings\",\"version\":1,\"profiles\":[]}", null}) {
            SettingsStore.ImportPreview pv = SettingsStore.previewImport(malo);
            assertFalse(pv.valid());
            s.applyImport(pv);
        }
        assertEquals(1, s.profiles().size());
    }

    @Test void contrasteYTextoSobreAcento() {
        assertEquals("#FFFFFF", Colors.onColor("#1B2A6B"));
        assertEquals("#16110E", Colors.onColor("#F6D365"));
        assertEquals("#00F0FF", Colors.normalize("#0ff0ff".replace("0ff0ff", "00f0ff")));
        assertNull(Colors.normalize("zzz"));
        assertEquals("#AABBCC", Colors.normalize("abc".equals("abc") ? "#ABC" : ""));
        assertTrue(Colors.contrast("#000000", "#FFFFFF") > 20);
        assertFalse(Palette.contrastWarnings("#FFFFFF", "#FFFFFF", "#EEEEEE", "#F0F0F0").isEmpty());
        assertTrue(Palette.contrastWarnings("#F5F4F0", "#FFFFFF", "#1C1F26", "#C64B1C").isEmpty());
    }

    @Test void resolucionDeTema() {
        SettingsStore s = new SettingsStore(new Mem());
        Palette.Resolved r = Palette.resolve(s, LocalTime.NOON);
        assertTrue(r.legacy());
        assertFalse(r.dark());
        s.set("appearance.autoThemeByTime", true);
        assertTrue(Palette.resolve(s, LocalTime.of(22, 0)).dark());
        assertFalse(Palette.resolve(s, LocalTime.of(12, 0)).dark());
        s.set("appearance.autoThemeByTime", false);
        s.set("appearance.themeId", "neon");
        Palette.Resolved n = Palette.resolve(s, LocalTime.NOON);
        assertTrue(n.dark());
        assertEquals("#00F0FF", n.accent());
        s.set("appearance.colors.bg", "#FFFFFF");
        assertFalse(Palette.resolve(s, LocalTime.NOON).dark());
    }

    @Test void jsonIdaYVuelta() {
        Map<String, Object> o = Json.parseObject("{\"a\":[1,2.5,true,null,\"x\\n\\u00e9\"],\"b\":{\"c\":-3}}");
        assertEquals(o, Json.parseObject(Json.pretty(o)));
        assertThrows(Json.JsonException.class, () -> Json.parse("[1,2"));
    }
}
