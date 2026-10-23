package com.agendapersonal.service;

import com.agendapersonal.model.Tarea;
import java.util.Collection;

/** Cálculo del avance de proyectos (lógica pura, sin UI ni BD). */
public final class ProgresoService {
    private ProgresoService() {}

    /** Resultado de cambiar el estado de una tarea: avance antes y después. */
    public record Resultado(Integer proyectoId, int antes, int despues, boolean completada) {
        public boolean cambio() { return antes != despues; }

        /** Ej.: "Tarea completada · Progreso: 17% → 33%". */
        public String mensaje(String nombreProyecto) {
            String ambito = nombreProyecto == null || nombreProyecto.isBlank() ? "" : " (" + nombreProyecto + ")";
            return (completada ? "Tarea completada" : "Tarea reabierta") + ambito
                + " · Progreso: " + antes + "% → " + despues + "%";
        }
    }

    /**
     * Porcentaje (0-100, redondeado) de tareas completadas del proyecto.
     * Con {@code proyectoId == null} calcula el avance global de todas las tareas.
     */
    public static int porcentaje(Integer proyectoId, Collection<Tarea> tareas) {
        int total = 0, hechas = 0;
        for (Tarea t : tareas) {
            if (proyectoId != null && !proyectoId.equals(t.getProyectoId())) continue;
            total++;
            if (t.estaCompletada()) hechas++;
        }
        return total == 0 ? 0 : (int) Math.round(100.0 * hechas / total);
    }
}
