package com.agendapersonal.service;

import com.agendapersonal.domain.model.BloqueTiempo;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.model.Tarea;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Asistente <b>local</b>: lee tus tareas, notas, bloques y hábitos y te dice por dónde empezar.
 *
 * <p>No usa internet ni manda nada a ningún sitio: son reglas sencillas que corren en tu equipo
 * con los datos que ya tienes en Compás. Cada suggestion trae el método de bloqueo que mejor encaja
 * con lo que tienes entre manos.</p>
 */
public final class AsistenteLocal {

    /** Una sugerencia con su explicación y el método de bloqueo que recomienda. */
    public record Sugerencia(String titulo, String detalle, String tono, String metodoId) {}

    /** Los datos que mira el asistente. */
    public record Contexto(List<Tarea> tareas, List<Nota> notas, List<BloqueTiempo> bloques,
                            List<Habito> habitos, Set<Integer> habitosCumplidos, LocalDate hoy) {}

    private AsistenteLocal() {}

    /**
     * Genera las sugerencias del día, de la más urgente a la menos.
     * Nunca devuelve null; si todo está en orden, devuelve una única sugerencia de calma.
     */
    public static List<Sugerencia> sugerir(Contexto ctx) {
        List<Sugerencia> out = new ArrayList<>();
        if (ctx == null) return out;
        LocalDate hoy = ctx.hoy() == null ? LocalDate.now() : ctx.hoy();
        List<Tarea> tareas = ctx.tareas() == null ? List.of() : ctx.tareas();

        List<Tarea> pendientes = new ArrayList<>();
        List<Tarea> vencidas = new ArrayList<>();
        List<Tarea> deHoy = new ArrayList<>();
        for (Tarea t : tareas) {
            if (t.getEstadoTipo() == null || t.getEstadoTipo().esCompletada()) continue;
            pendientes.add(t);
            if (t.getFecha() == null) continue;
            if (t.getFecha().isBefore(hoy)) vencidas.add(t);
            else if (t.getFecha().isEqual(hoy)) deHoy.add(t);
        }

        // 1. Lo vencido manda: es lo que más cuesta dejar atrás.
        if (!vencidas.isEmpty()) {
            Tarea t = vencidas.get(0);
            out.add(new Sugerencia("Empieza por lo que venció",
                vencidas.size() == 1
                    ? "«" + t.getTitulo() + "» se pasó de fecha. Son 25 minutos y te lo quitas de encima."
                    : "Tienes " + vencidas.size() + " tareas vencidas. La más antigua es «" + t.getTitulo()
                        + "». Un pomodoro de 25 min por cada una.",
                "rose", "pomodoro"));
        }

        // 2. Las notas con casillas sin marcar.
        List<ListaPendientes.Item> casillas = ListaPendientes.desdeNotas(ctx.notas() == null ? List.of() : ctx.notas());
        long sinHacer = casillas.stream().filter(x -> !x.hecho()).count();
        if (sinHacer > 0) {
            out.add(new Sugerencia("Hay " + sinHacer + (sinHacer == 1 ? " pendiente" : " pendientes") + " en tus notas",
                "Las casillas de tus notas («- [ ] algo») también sirven como lista de trabajo en la pantalla de bloqueo.",
                "amber", "timeboxing"));
        }

        // 3. Los hábitos del día.
        if (ctx.habitos() != null && !ctx.habitos().isEmpty()) {
            Set<Integer> cumplidos = ctx.habitosCumplidos() == null ? Set.of() : ctx.habitosCumplidos();
            int pendientesHabito = 0;
            for (Habito h : ctx.habitos())
                if (h.isActivo() && !cumplidos.contains(h.getId())) pendientesHabito++;
            if (pendientesHabito > 0) {
                out.add(new Sugerencia("Te faltan " + pendientesHabito + (pendientesHabito == 1 ? " hábito" : " hábitos") + " por marcar",
                    "Márcalos mientras trabajas: un hábito pequeño al principio del día cuesta menos.",
                    "green", "pomodoro"));
            }
        }

        // 4. Qué método encaja con lo que tienes entre manos.
        if (pendientes.size() >= 4) {
            out.add(new Sugerencia("Tienes muchas cosas abiertas (" + pendientes.size() + ")",
                "Con muchas tareas pendientes funciona mejor trabajar en pomodoros de 25 min y pasar de una a otra.",
                "blue", "pomodoro"));
        } else if (pendientes.size() == 1) {
            out.add(new Sugerencia("Tienes una sola cosa pendiente",
                "Es buen momento para un bloque largo: deep work de 60 a 90 minutos sin interrupciones.",
                "blue", "deep-rango"));
        } else if (pendientes.size() >= 2) {
            out.add(new Sugerencia("Tienes " + pendientes.size() + " cosas pendientes",
                "Prueba con bloques de 52 minutos de trabajo y 17 de descanso: da tiempo a entrar en cada tarea.",
                "blue", "52-17"));
        }

        // 5. El día en el calendario: ¿tienes bloqueado el tiempo?
        List<BloqueTiempo> bloques = ctx.bloques() == null ? List.of() : ctx.bloques();
        List<BloqueTiempo> hoyBloques = new ArrayList<>();
        for (BloqueTiempo b : bloques) if (hoy.toString().equals(b.getFecha())) hoyBloques.add(b);
        if (hoyBloques.isEmpty() && !pendientes.isEmpty()) {
            out.add(new Sugerencia("Hoy no tienes ningún bloque reservado",
                "Reserva un bloque de enfoque en el calendario: lo que tiene hora, se hace.",
                "orange", "pomodoro"));
        } else if (!hoyBloques.isEmpty()) {
            LocalTime ahora = LocalTime.now();
            BloqueTiempo enCurso = null, siguiente = null;
            for (BloqueTiempo b : hoyBloques) {
                LocalTime ini = parseo(b.getHoraInicio()), fin = parseo(b.getHoraFin());
                if (ini == null || fin == null) continue;
                if (!ahora.isBefore(ini) && ahora.isBefore(fin)) enCurso = b;
                if (ini.isAfter(ahora) && (siguiente == null || ini.isBefore(parseo(siguiente.getHoraInicio())))) siguiente = b;
            }
            if (enCurso != null) {
                out.add(new Sugerencia("Estás dentro de un bloque de enfoque",
                    "«" + enCurso.getTitulo() + "» va de " + enCurso.getHoraInicio() + " a " + enCurso.getHoraFin()
                        + ". Es buen momento para abrir la pantalla de bloqueo.",
                    "orange", enCurso.getCategoria().equalsIgnoreCase("Estudio") ? "deep-rango" : "pomodoro"));
            } else if (siguiente != null) {
                out.add(new Sugerencia("Tu próximo bloque empieza a las " + siguiente.getHoraInicio(),
                    "«" + siguiente.getTitulo() + "». Tienes un rato para adelantar algo antes.",
                    "orange", "timeboxing"));
            }
        }

        // 6. Sin nada pendiente: un mensaje tranquilo.
        if (out.isEmpty()) {
            out.add(new Sugerencia("Estás al día",
                pendientes.isEmpty()
                    ? "No hay tareas pendientes. Buen momento para avanzar un proyecto con un bloque largo de deep work."
                    : "Nada urgente por ahora. Puedes aprovechar para ordenar tus notas o preparar la semana.",
                "green", "deep-rango"));
        }
        return out;
    }

    private static LocalTime parseo(String hhmm) {
        try { return LocalTime.parse(hhmm); } catch (RuntimeException e) { return null; }
    }
}
