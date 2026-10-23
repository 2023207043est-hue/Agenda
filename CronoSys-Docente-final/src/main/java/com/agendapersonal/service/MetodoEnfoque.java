package com.agendapersonal.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Método de enfoque: cuánto se trabaja, cuánto se descansa y cada cuántas sesiones hay un descanso largo.
 *
 * <p>Los tiempos pueden ser un valor exacto ({@code min == max}) o un <b>rango</b>: por ejemplo
 * "60–90 min de trabajo y 10–20 de descanso". Cuando el método tiene un rango, al empezar la sesión
 * se elige un valor concreto dentro de él y se muestra en pantalla, de modo que siempre sabes
 * cuánto vas a trabajar.</p>
 *
 * <p>Tiempos en minutos. {@code libre} = flowtime (se trabaja sin límite y el descanso se calcula al terminar).</p>
 */
public record MetodoEnfoque(String id, String nombre, String descripcion, String usoComun,
                             int trabajoMin, int trabajoMax,
                             int descansoMin, int descansoMax,
                             int sesionesPorCiclo,
                             int descansoLargoMin, int descansoLargoMax,
                             boolean libre, boolean propio) {

    private static final Random AZAR = new Random();

    public static final List<MetodoEnfoque> PREDEFINIDOS = List.of(
        new MetodoEnfoque("pomodoro", "Pomodoro",
            "Sesiones cortas de 25 min con 5 min de descanso; tras 4 sesiones, un descanso largo de 15 a 30 min.",
            "Estudiar, tareas administrativas, empezar cuando cuesta arrancar.",
            25, 25, 5, 5, 4, 15, 30, false, false),
        new MetodoEnfoque("52-17", "Método 52/17",
            "52 minutos de trabajo y 17 de descanso, sin descanso largo.",
            "Trabajo de oficina sostenido: redactar, programar, analizar.",
            52, 52, 17, 17, 0, 0, 0, false, false),
        new MetodoEnfoque("deep-60", "Deep work 60/10",
            "Bloque largo de 60 min sin interrupciones y 10 min de pausa.",
            "Tareas que piden concentración profunda: escribir, diseñar, resolver problemas.",
            60, 60, 10, 10, 0, 0, 0, false, false),
        new MetodoEnfoque("deep-90", "Deep work 90/20",
            "Bloque largo de 90 min y 20 min de pausa.",
            "Proyectos grandes: tesis, código complejo, estudio intenso.",
            90, 90, 20, 20, 0, 0, 0, false, false),
        new MetodoEnfoque("deep-rango", "Deep work 60–90",
            "Bloque largo de entre 60 y 90 min de trabajo y entre 10 y 20 de descanso: la duración se "
                + "sortea al empezar cada sesión, así nunca te cansas de la misma manera.",
            "Trabajo profundo de aquí a un rato, con pausas en el punto justo.",
            60, 90, 10, 20, 0, 0, 0, false, false),
        new MetodoEnfoque("ultradiano", "Ciclos ultradianos",
            "Ciclos de 90 min de trabajo y 20 de descanso siguiendo el ritmo natural de energía; "
                + "descanso largo de 30 min cada 4 ciclos.",
            "Jornadas largas cuidando la energía a lo largo del día.",
            90, 90, 20, 20, 4, 30, 30, false, false),
        new MetodoEnfoque("flowtime", "Flowtime",
            "Trabajas hasta que pierdas el foco, sin cronómetro regresivo. Al terminar, el descanso se calcula "
                + "(un quinto de lo trabajado, mínimo 5 min).",
            "Creatividad y tareas donde interrumpirte rompe el ritmo.",
            0, 0, 5, 5, 0, 0, 0, true, false),
        new MetodoEnfoque("timeboxing", "Timeboxing",
            "Asignas una caja de tiempo fija a una tarea (por defecto 45 min) y paras al terminar, esté o no lista.",
            "Evitar el perfeccionismo y limitar tareas que se alargan.",
            45, 45, 5, 5, 0, 0, 0, false, false)
    );

    /** Técnicas pensadas para las tareas típicas de preparación y evaluación docente. */
    public static final List<MetodoEnfoque> DOCENTES = List.of(
        new MetodoEnfoque("docente-preparacion", "Preparación de clase 50/10",
            "50 minutos para diseñar una clase y 10 minutos para descansar.",
            "Preparar materiales, secuencias y actividades de clase.", 50, 50, 10, 10, 0, 0, 0, false, false),
        new MetodoEnfoque("docente-evaluacion", "Corrección 40/10",
            "40 minutos de revisión concentrada y 10 minutos de pausa.",
            "Revisar trabajos, retroalimentar y registrar resultados.", 40, 40, 10, 10, 0, 0, 0, false, false),
        new MetodoEnfoque("docente-planeacion", "Planeación profunda 90/20",
            "90 minutos de trabajo profundo y 20 minutos de recuperación.",
            "Planificación semanal, diseño curricular y proyectos docentes.", 90, 90, 20, 20, 0, 0, 0, false, false)
    );

    // ------------------------------------------------------------------ acceso a los tiempos

    /** Minutos de trabajo (el valor más corto del rango). */
    public int trabajo() { return trabajoMin; }
    /** Minutos de descanso (el valor más corto del rango). */
    public int descanso() { return descansoMin; }
    /** Minutos de descanso largo (el valor más corto del rango). */
    public int descansoLargo() { return descansoLargoMin; }

    public boolean rangoTrabajo() { return trabajoMax > trabajoMin; }
    public boolean rangoDescanso() { return descansoMax > descansoMin; }
    public boolean rangoDescansoLargo() { return descansoLargoMax > descansoLargoMin; }

    /** ¿Alguno de los tres tiempos es un rango y no un valor exacto? */
    public boolean tieneRangos() { return rangoTrabajo() || rangoDescanso() || rangoDescansoLargo(); }

    // ------------------------------------------------------------------ creación

    /** Método propio con tiempos exactos (min = max). */
    public static MetodoEnfoque personalizado(String nombre, int trabajo, int descanso, int sesiones, int largo) {
        return personalizado(nombre, trabajo, trabajo, descanso, descanso, sesiones, largo, largo);
    }

    /**
     * Método propio con rangos. Si el máximo es menor que el mínimo se intercambian;
     * los valores se acotan a límites razonables.
     */
    public static MetodoEnfoque personalizado(String nombre,
                                              int trabajoMin, int trabajoMax, int descansoMin, int descansoMax,
                                              int sesiones, int largoMin, int largoMax) {
        int t1 = clamp(trabajoMin, 1, 240), t2 = clamp(trabajoMax, 1, 240);
        if (t2 < t1) { int x = t1; t1 = t2; t2 = x; }
        int d1 = clamp(descansoMin, 0, 120), d2 = clamp(descansoMax, 0, 120);
        if (d2 < d1) { int x = d1; d1 = d2; d2 = x; }
        int l1 = clamp(largoMin, 0, 120), l2 = clamp(largoMax, 0, 120);
        if (l2 < l1) { int x = l1; l1 = l2; l2 = x; }
        String n = nombre == null || nombre.isBlank() ? "Personalizado" : nombre.trim();
        return new MetodoEnfoque("custom-" + Math.abs((n + t1 + d1).hashCode()), n,
            "Método propio: " + rangoTexto(t1, t2) + " de trabajo y " + rangoTexto(d1, d2) + " de descanso.",
            "Lo defines tú.", t1, t2, d1, d2, clamp(sesiones, 0, 12), l1, l2, false, true);
    }

    /** Método propio que copia otro (para duplicar y ajustar). */
    public MetodoEnfoque comoPropio(String nombreNuevo) {
        String n = nombreNuevo == null || nombreNuevo.isBlank() ? nombre : nombreNuevo.trim();
        return new MetodoEnfoque("custom-" + Math.abs((n + trabajoMin + descansoMin).hashCode()), n,
            descripcion, usoComun, trabajoMin, trabajoMax, descansoMin, descansoMax,
            sesionesPorCiclo, descansoLargoMin, descansoLargoMax, libre, true);
    }

    private static int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    /** "25 min" o "60–90 min". */
    public static String rangoTexto(int min, int max) {
        return min == max ? min + " min" : min + "–" + max + " min";
    }

    /** Elige un valor concreto dentro de un rango (o el único valor si no hay rango). */
    public static int elegir(int min, int max) {
        if (max <= min) return min;
        return min + AZAR.nextInt(max - min + 1);
    }

    // ------------------------------------------------------------------ consulta

    public static MetodoEnfoque porId(String id, List<MetodoEnfoque> propios) {
        for (MetodoEnfoque m : PREDEFINIDOS) if (m.id.equals(id)) return m;
        for (MetodoEnfoque m : propios) if (m.id.equals(id)) return m;
        return PREDEFINIDOS.get(0);
    }

    /** Resumen corto para la interfaz: "25 min trabajo · 5 min descanso · cada 4 sesiones, 15–30 min". */
    public String resumen() {
        if (libre) return "Sin límite de trabajo";
        String s = rangoTexto(trabajoMin, trabajoMax) + " trabajo · " + rangoTexto(descansoMin, descansoMax) + " descanso";
        if (sesionesPorCiclo > 0 && descansoLargoMax > 0)
            s += " · cada " + sesionesPorCiclo + " sesiones, descanso largo de " + rangoTexto(descansoLargoMin, descansoLargoMax);
        return s;
    }

    // ---- guardado como mapa (para los ajustes JSON) ----

    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", nombre); m.put("focus", trabajoMin); m.put("focusMax", trabajoMax);
        m.put("break", descansoMin); m.put("breakMax", descansoMax);
        m.put("rounds", sesionesPorCiclo);
        m.put("longBreak", descansoLargoMin); m.put("longBreakMax", descansoLargoMax);
        m.put("desc", descripcion); m.put("use", usoComun);
        return m;
    }

    public static List<MetodoEnfoque> desdeMapas(Object raw) {
        List<MetodoEnfoque> out = new ArrayList<>();
        if (!(raw instanceof List<?> l)) return out;
        for (Object o : l) {
            if (!(o instanceof Map<?, ?> m)) continue;
            // Sin "focus" y "break" la entrada está dañada: se omite en lugar de inventar un método.
            if (!(m.get("focus") instanceof Number) || !(m.get("break") instanceof Number)) continue;
            try {
                int foco = numero(m.get("focus"), 25);
                int focoMax = numero(m.get("focusMax"), foco);
                int descanso = numero(m.get("break"), 5);
                int descansoMax = numero(m.get("breakMax"), descanso);
                int rondas = numero(m.get("rounds"), 0);
                int largo = numero(m.get("longBreak"), 0);
                int largoMax = numero(m.get("longBreakMax"), largo);
                String nombre = m.get("name") instanceof String s ? s : "Personalizado";
                MetodoEnfoque base = personalizado(nombre, foco, focoMax, descanso, descansoMax, rondas, largo, largoMax);
                String id = m.get("id") instanceof String sid ? sid : base.id();
                String desc = m.get("desc") instanceof String s && !s.isBlank() ? s : base.descripcion();
                String uso = m.get("use") instanceof String s && !s.isBlank() ? s : base.usoComun();
                out.add(new MetodoEnfoque(id, base.nombre, desc, uso, base.trabajoMin, base.trabajoMax,
                    base.descansoMin, base.descansoMax, base.sesionesPorCiclo,
                    base.descansoLargoMin, base.descansoLargoMax, false, true));
            } catch (RuntimeException ignored) { /* entrada dañada: se omite */ }
        }
        return out;
    }

    private static int numero(Object o, int porDefecto) {
        return o instanceof Number n ? n.intValue() : porDefecto;
    }
}
