package com.agendapersonal.controller;

import com.agendapersonal.dao.*;
import com.agendapersonal.domain.model.Proyecto;
import com.agendapersonal.infrastructure.repo.ProyectoDAO;
import com.agendapersonal.model.*;
import com.agendapersonal.service.AgendaService;
import com.agendapersonal.service.ConflictoHorario;
import com.agendapersonal.service.ProgresoService;
import javafx.application.Platform;
import javafx.collections.*;
import java.time.LocalDate;
import java.util.List;

public final class MainController {
    private final EventoDAO eventoDAO=new EventoDAO();
    private final TareaDAO tareaDAO=new TareaDAO();
    private final ProyectoDAO proyectoDAO=new ProyectoDAO();
    private final ObservableList<Evento> eventos=FXCollections.observableArrayList();
    private final ObservableList<Tarea> tareas=FXCollections.observableArrayList();

    public MainController(){cargarTodo();}
    public void cargarTodo(){eventos.setAll(eventoDAO.listarTodos()); tareas.setAll(tareaDAO.listarTodas());}

    /** Equivalente a AJAX: recarga SQLite en hilo de fondo sin congelar la UI. */
    public void cargarTodoAsync() { cargarTodoAsync(null); }

    public void cargarTodoAsync(Runnable onDone) {
        Thread t = new Thread(() -> {
            List<Evento> ev = eventoDAO.listarTodos();
            List<Tarea> ta = tareaDAO.listarTodas();
            Platform.runLater(() -> {
                // Solo se reemplazan las listas si algo cambió de verdad: antes se recargaban cada 15 s
                // y las tablas perdían la selección y el desplazamiento aunque nada hubiera cambiado.
                boolean cambioEventos = !firmaEventos(ev).equals(firmaEventos(eventos));
                boolean cambioTareas = !firmaTareas(ta).equals(firmaTareas(tareas));
                if (cambioEventos) eventos.setAll(ev);
                if (cambioTareas) tareas.setAll(ta);
                if ((cambioEventos || cambioTareas) && alCambioExterno != null) alCambioExterno.run();
                if (onDone != null) onDone.run();
            });
        }, "db-refresh");
        t.setDaemon(true);
        t.start();
    }

    private Runnable alCambioExterno;

    /** Se ejecuta cuando la recarga periódica encuentra cambios hechos fuera de esta ventana. */
    public void setAlCambioExterno(Runnable r) { alCambioExterno = r; }

    private static List<String> firmaEventos(List<Evento> l) {
        return l.stream().map(e -> e.getId() + "|" + e.getTitulo() + "|" + e.getFecha() + "|" + e.getHoraInicio()
            + "|" + e.getHoraFin() + "|" + e.getCategoria() + "|" + e.getPrioridad() + "|" + e.getDescripcion()
            + "|" + e.getCalendarioId()).toList();
    }

    private static List<String> firmaTareas(List<Tarea> l) {
        return l.stream().map(t -> t.getId() + "|" + t.getTitulo() + "|" + t.getEstado() + "|" + t.getFecha()
            + "|" + t.getPrioridad() + "|" + t.getProyectoId() + "|" + t.getDescripcion()).toList();
    }

    public boolean guardarEvento(Evento e){boolean ok=eventoDAO.insertar(e);if(ok)cargarTodo();return ok;}
    public boolean actualizarEvento(Evento e){boolean ok=eventoDAO.actualizar(e);if(ok)cargarTodo();return ok;}
    public boolean eliminarEvento(int id){boolean ok=eventoDAO.eliminar(id);if(ok)cargarTodo();return ok;}
    public List<Evento> eventosDelDia(LocalDate f){return eventoDAO.listarPorFecha(f);}
    public ObservableList<Evento> getEventos(){return eventos;}

    public boolean guardarTarea(Tarea t){boolean ok=tareaDAO.insertar(t);if(ok)cargarTodo();return ok;}
    public boolean actualizarTarea(Tarea t){boolean ok=tareaDAO.actualizar(t);if(ok)cargarTodo();return ok;}
    public boolean eliminarTarea(int id){boolean ok=tareaDAO.eliminar(id);if(ok)cargarTodo();return ok;}
    public ObservableList<Tarea> getTareas(){return tareas;}

    // ---- Reglas de negocio expuestas a la UI (la lógica vive en service/) ----

    /** Cruces del evento con la agenda actual (vacío si no hay conflicto). */
    public List<ConflictoHorario> conflictosPara(Evento e){
        return AgendaService.detectarConflictos(e, eventos);
    }

    /** Primer hueco libre de {@code minutos} ese día entre las 07:00 y las 22:00. */
    public java.util.Optional<java.time.LocalTime[]> huecoLibre(LocalDate dia, int minutos){
        return AgendaService.sugerirHueco(dia, eventos, minutos,
            java.time.LocalTime.of(7,0), java.time.LocalTime.of(22,0));
    }

    /**
     * Cambia el estado de una tarea, guarda y devuelve el avance antes → después
     * (del proyecto de la tarea, o global si no tiene proyecto). Vacío si no se pudo guardar.
     */
    public java.util.Optional<ProgresoService.Resultado> cambiarEstadoTarea(Tarea t, EstadoTarea nuevo){
        Integer pid=t.getProyectoId();
        int antes=ProgresoService.porcentaje(pid,tareas);
        EstadoTarea previo=t.getEstadoTipo();
        t.setEstado(nuevo);
        if(!tareaDAO.actualizar(t)){ t.setEstado(previo); return java.util.Optional.empty(); }
        cargarTodo();
        int despues=ProgresoService.porcentaje(pid,tareas);
        return java.util.Optional.of(new ProgresoService.Resultado(pid,antes,despues,nuevo.esCompletada()));
    }

    /** Alterna Completada ↔ Pendiente. */
    public java.util.Optional<ProgresoService.Resultado> alternarCompletada(Tarea t){
        return cambiarEstadoTarea(t, t.estaCompletada()?EstadoTarea.PENDIENTE:EstadoTarea.COMPLETADA);
    }

    /** Texto del toast de progreso, con el nombre del proyecto si lo tiene. */
    public String mensajeProgreso(ProgresoService.Resultado r){
        String nombre=null;
        if(r.proyectoId()!=null)
            for(Proyecto p:proyectoDAO.listar()) if(p.getId()==r.proyectoId()) nombre=p.getNombre();
        return r.mensaje(nombre);
    }

    public List<Proyecto> getProyectos(){return proyectoDAO.listar();}
}
