package com.agendapersonal.service;

import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Horas;
import com.agendapersonal.model.Tarea;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Une en una sola línea de tiempo todo lo que el usuario guarda en Compás:
 * eventos, trabajos, tareas, hábitos y cumpleaños. Lógica pura (sin UI ni BD) y con pruebas.
 *
 * Reglas de segmentación:
 *  - TRABAJO: eventos de categoría "Trabajo" + tareas que pertenecen a un proyecto.
 *  - EVENTO : el resto de eventos.
 *  - TAREA  : tareas sin proyecto.
 *  - HABITO : DIARIO = todos los días; SEMANAL = aparece los lunes. Solo hábitos activos.
 *  - CUMPLE : se repite cada año (el 29-feb cae el 28-feb en años no bisiestos).
 */
public final class CalendarioUnificadoService {
    private CalendarioUnificadoService() {}

    /** Instantánea de datos sobre la que se calcula (así la UI carga una sola vez por refresco). */
    public record Datos(Collection<Evento> eventos, Collection<Tarea> tareas, Collection<Habito> habitos,
                        Collection<Contacto> contactos, Map<String, Set<Integer>> habitosCumplidos) {
        public static Datos vacio() { return new Datos(List.of(), List.of(), List.of(), List.of(), Map.of()); }
    }

    public static boolean esTrabajo(Evento e) { return "Trabajo".equalsIgnoreCase(e.getCategoria()); }

    public static List<Entrada> delDia(LocalDate dia, Datos d, Set<TipoEntrada> visibles) {
        List<Entrada> r = new ArrayList<>();

        for (Evento e : d.eventos()) {
            if (!dia.equals(e.getFecha())) continue;
            TipoEntrada t = esTrabajo(e) ? TipoEntrada.TRABAJO : TipoEntrada.EVENTO;
            if (!visibles.contains(t)) continue;
            try {
                r.add(new Entrada(t, e.getTitulo(), e.getDescripcion(), dia, e.getInicio(), e.getFin(), false, e));
            } catch (DateTimeParseException ex) {   // hora corrupta en BD: se muestra como todo el día
                r.add(new Entrada(t, e.getTitulo(), e.getDescripcion(), dia, null, null, false, e));
            }
        }

        for (Tarea t : d.tareas()) {
            if (!dia.equals(t.getFecha())) continue;
            TipoEntrada tipo = t.getProyectoId() != null ? TipoEntrada.TRABAJO : TipoEntrada.TAREA;
            if (!visibles.contains(tipo)) continue;
            r.add(new Entrada(tipo, t.getTitulo(), t.getDescripcion(), dia, null, null, t.estaCompletada(), t));
        }

        if (visibles.contains(TipoEntrada.HABITO)) {
            Set<Integer> hechos = d.habitosCumplidos().getOrDefault(dia.toString(), Set.of());
            for (Habito h : d.habitos()) {
                if (!h.isActivo() || !aplicaHabito(h, dia)) continue;
                r.add(new Entrada(TipoEntrada.HABITO, h.getNombre(), h.getDescripcion(), dia, null, null,
                    hechos.contains(h.getId()), h));
            }
        }

        if (visibles.contains(TipoEntrada.CUMPLE)) {
            for (Contacto c : d.contactos()) {
                LocalDate nac = fecha(c.getCumpleanos());
                if (nac == null) continue;
                if (MonthDay.from(nac).atYear(dia.getYear()).equals(dia)) {
                    int edad = dia.getYear() - nac.getYear();
                    r.add(new Entrada(TipoEntrada.CUMPLE, "Cumpleaños de " + c.getNombre(),
                        edad > 0 ? "Cumple " + edad + " años" : "", dia, null, null, false, c));
                }
            }
        }

        r.sort(Comparator.comparing((Entrada e) -> !e.todoElDia())
            .thenComparing(e -> e.todoElDia() ? 0 : e.minutoInicio())
            .thenComparing(e -> e.tipo().ordinal())
            .thenComparing(Entrada::titulo, String.CASE_INSENSITIVE_ORDER));
        return r;
    }

    public static boolean aplicaHabito(Habito h, LocalDate dia) {
        if ("SEMANAL".equalsIgnoreCase(h.getFrecuencia())) return dia.getDayOfWeek() == DayOfWeek.MONDAY;
        return true;
    }

    /** Cuántas entradas de cada tipo hay en el rango (para el punto indicador del mini-calendario). */
    public static boolean hayAlgo(LocalDate dia, Datos d, Set<TipoEntrada> visibles) {
        for (Entrada e : delDia(dia, d, visibles))
            if (e.tipo() != TipoEntrada.HABITO) return true;   // los hábitos están todos los días: no marcan punto
        return false;
    }

    // ---------- diseño de la cuadrícula horaria ----------

    /** Posición de una entrada con hora dentro de su día: carril (columna) y total de carriles solapados. */
    public record Carril(Entrada entrada, int carril, int carriles) {}

    /** Reparte en columnas los eventos que se solapan (como Google: lado a lado). */
    public static List<Carril> asignarCarriles(List<Entrada> conHora) {
        List<Entrada> orden = new ArrayList<>(conHora);
        orden.sort(Comparator.comparingInt(Entrada::minutoInicio).thenComparingInt(Entrada::minutoFin));
        List<Carril> out = new ArrayList<>();
        List<Entrada> grupo = new ArrayList<>();
        int finGrupo = -1;
        for (Entrada e : orden) {
            if (!grupo.isEmpty() && e.minutoInicio() >= finGrupo) { volcar(grupo, out); grupo.clear(); finGrupo = -1; }
            grupo.add(e);
            finGrupo = Math.max(finGrupo, e.minutoFin());
        }
        if (!grupo.isEmpty()) volcar(grupo, out);
        return out;
    }

    private static void volcar(List<Entrada> grupo, List<Carril> out) {
        List<Integer> finPorCarril = new ArrayList<>();
        int[] asignado = new int[grupo.size()];
        for (int i = 0; i < grupo.size(); i++) {
            Entrada e = grupo.get(i);
            int c = 0;
            while (c < finPorCarril.size() && finPorCarril.get(c) > e.minutoInicio()) c++;
            if (c == finPorCarril.size()) finPorCarril.add(e.minutoFin()); else finPorCarril.set(c, e.minutoFin());
            asignado[i] = c;
        }
        for (int i = 0; i < grupo.size(); i++) out.add(new Carril(grupo.get(i), asignado[i], finPorCarril.size()));
    }

    private static LocalDate fecha(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }

    /** Hora válida o null (útil para los formularios). */
    static java.time.LocalTime hora(String s) { return Horas.esValida(s) ? Horas.parsear(s) : null; }
}
