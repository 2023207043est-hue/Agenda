package com.agendapersonal.ui;

import com.agendapersonal.ui.FontPreferences;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.Proyecto;
import com.agendapersonal.infrastructure.repo.ProyectoDAO;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.ProgresoService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Locale;

public final class TaskViews {
    private TaskViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void style(Scene sc) {
        com.agendapersonal.util.Ui.decorarDialogo(sc);
    }

    public static void form(Tarea old, MainController c, Runnable onSaved) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(420);
        TextField title = new TextField(old == null ? "" : old.getTitulo());
        TextArea desc = new TextArea(old == null ? "" : old.getDescripcion());
        DatePicker date = new DatePicker(old == null ? LocalDate.now() : old.getFecha());
        ComboBox<String> state = new ComboBox<>(), pri = new ComboBox<>();
        state.getItems().addAll("Pendiente", "En progreso", "Completada");
        pri.getItems().addAll("Baja", "Media", "Alta");
        state.setValue(old == null ? "Pendiente" : old.getEstado());
        pri.setValue(old == null ? "Media" : old.getPrioridad());
        // Proyecto: antes se perdía el vínculo al editar una tarea; ahora se conserva y se puede cambiar.
        ComboBox<Proyecto> proy = new ComboBox<>();
        Proyecto sinProyecto = new Proyecto(0, "(Sin lista)", "", "", null, null, null) {
            @Override public String toString() { return getNombre(); }
        };
        proy.getItems().add(sinProyecto);
        proy.getItems().addAll(c.getProyectos());
        proy.setValue(sinProyecto);
        if (old != null && old.getProyectoId() != null)
            for (Proyecto p : proy.getItems()) if (p.getId() == old.getProyectoId()) proy.setValue(p);
        // Hora opcional, destacada y vínculos con un evento o un bloque de time blocking.
        CheckBox conHora = new CheckBox("Ponerle una hora");
        TextField hora = new TextField(old != null && old.getHora() != null ? old.getHora() : "09:00");
        hora.setDisable(!(old != null && old.getHora() != null));
        conHora.setSelected(old != null && old.getHora() != null);
        conHora.selectedProperty().addListener((o, a, v) -> hora.setDisable(!v));
        CheckBox destacada = new CheckBox("★ Destacar esta tarea");
        destacada.setSelected(old != null && old.isDestacada());
        ComboBox<String[]> evento = new ComboBox<>(), bloque = new ComboBox<>();
        evento.getItems().add(new String[]{"0", "(Sin evento)"});
        for (var ev : c.getEventos()) evento.getItems().add(new String[]{String.valueOf(ev.getId()), ev.getFecha() + " " + ev.getHoraInicio() + " · " + ev.getTitulo()});
        bloque.getItems().add(new String[]{"0", "(Sin bloque)"});
        for (var b : new com.agendapersonal.infrastructure.repo.BloqueDAO().proximos())
            bloque.getItems().add(new String[]{String.valueOf(b.getId()), b.getFecha() + " " + b.getHoraInicio() + "–" + b.getHoraFin() + " · " + b.getTitulo()});
        javafx.util.StringConverter<String[]> conv = new javafx.util.StringConverter<>() {
            @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
            @Override public String[] fromString(String s) { return null; }
        };
        evento.setConverter(conv);
        bloque.setConverter(conv);
        evento.setValue(evento.getItems().get(0));
        bloque.setValue(bloque.getItems().get(0));
        if (old != null && old.getEventoId() != null) for (String[] v : evento.getItems()) if (v[0].equals(String.valueOf(old.getEventoId()))) evento.setValue(v);
        if (old != null && old.getBloqueId() != null) for (String[] v : bloque.getItems()) if (v[0].equals(String.valueOf(old.getBloqueId()))) bloque.setValue(v);
        Button save = new Button(old == null ? "Guardar" : "Actualizar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(e -> {
            if (conHora.isSelected() && !hora.getText().trim().matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                new Alert(Alert.AlertType.WARNING, "La hora debe tener el formato HH:mm, por ejemplo 09:30.").showAndWait();
                return;
            }
            if (title.getText().isBlank() || date.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            Proyecto ps = proy.getValue();
            Integer pid = (ps == null || ps.getId() == 0) ? null : ps.getId();
            Tarea t;
            try {
                t = new Tarea(old == null ? 0 : old.getId(), title.getText().trim(), desc.getText().trim(),
                    state.getValue(), date.getValue(), pri.getValue(), pid);
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
                return;
            }
            t.setHora(conHora.isSelected() ? hora.getText().trim() : null);
            t.setDestacada(destacada.isSelected());
            t.setEventoId(evento.getValue() == null || evento.getValue()[0].equals("0") ? null : Integer.valueOf(evento.getValue()[0]));
            t.setBloqueId(bloque.getValue() == null || bloque.getValue()[0].equals("0") ? null : Integer.valueOf(bloque.getValue()[0]));
            int antes = ProgresoService.porcentaje(pid, c.getTareas());
            boolean eraCompletada = old != null && old.estaCompletada();
            boolean ok = old == null ? c.guardarTarea(t) : c.actualizarTarea(t);
            if (ok) {
                d.close();
                onSaved.run();
                Notifier.success(old == null ? "Tarea creada" : "Tarea actualizada", t.getTitulo()
                    + (t.getFecha() != null ? " · " + com.agendapersonal.util.Ui.friendlyDate(t.getFecha()) : ""));
                if (t.estaCompletada() != eraCompletada) {
                    int despues = ProgresoService.porcentaje(pid, c.getTareas());
                    Toast.mostrarGlobal(c.mensajeProgreso(new ProgresoService.Resultado(pid, antes, despues, t.estaCompletada())));
                }
            }
            else new Alert(Alert.AlertType.ERROR, "No se pudo guardar la tarea.").showAndWait();
        });
        cancel.setOnAction(e -> d.close());
        r.getChildren().addAll(new Label(old == null ? "Nueva tarea" : "Editar tarea"), new Label("Título"), title,
            new Label("Descripción"), desc, new Label("Fecha"), date, conHora, hora, new Label("Estado"), state,
            new Label("Prioridad"), pri, new Label("Lista"), proy, destacada,
            new Label("Vincular a un evento del calendario"), evento, new Label("Vincular a un bloque de time blocking"), bloque,
            new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        style(d.getScene());
        d.showAndWait();
    }

    public static void manage(MainController c, Runnable onChanged) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(760);
        Label title = new Label("Gestión de tareas");
        FontPreferences.bind(title, FontWeight.BOLD, 20);
        TextField search = new TextField();
        search.setPromptText("Buscar por título o descripción...");
        ComboBox<String> state = new ComboBox<>();
        state.getItems().addAll("Todos", "Pendiente", "En progreso", "Completada");
        state.setValue("Todos");
        ComboBox<String> pri = new ComboBox<>();
        pri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        pri.setValue("Todas");
        HBox filters = new HBox(8, search, state, pri);
        HBox.setHgrow(search, Priority.ALWAYS);
        ListView<Tarea> list = new ListView<>();
        Runnable filter = () -> {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            list.getItems().setAll(c.getTareas().stream()
                .filter(x -> q.isBlank() || x.getTitulo().toLowerCase(Locale.ROOT).contains(q)
                    || (x.getDescripcion() != null && x.getDescripcion().toLowerCase(Locale.ROOT).contains(q)))
                .filter(x -> "Todos".equals(state.getValue()) || x.getEstado().equals(state.getValue()))
                .filter(x -> "Todas".equals(pri.getValue()) || x.getPrioridad().equals(pri.getValue())).toList());
        };
        search.textProperty().addListener((o, old, n) -> filter.run());
        state.setOnAction(e -> filter.run());
        pri.setOnAction(e -> filter.run());
        filter.run();
        // En vivo: re-filtra solo cuando cambian las tareas
        javafx.collections.ListChangeListener<Tarea> live =
            ch -> javafx.application.Platform.runLater(filter::run);
        c.getTareas().addListener(live);
        d.setOnHidden(e -> c.getTareas().removeListener(live));
        Button add = new Button("Nueva tarea"), ed = new Button("Editar"), del = new Button("Eliminar"), close = new Button("Cerrar");
        add.setOnAction(e -> { form(null, c, () -> { filter.run(); onChanged.run(); }); });
        ed.setOnAction(e -> {
            Tarea x = list.getSelectionModel().getSelectedItem();
            if (x != null) { form(x, c, () -> { filter.run(); onChanged.run(); }); }
        });
        del.setOnAction(e -> {
            Tarea x = list.getSelectionModel().getSelectedItem();
            if (x != null) {
                Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + x.getTitulo() + "\"?");
                if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (c.eliminarTarea(x.getId())) Notifier.deleted("Tarea eliminada", x.getTitulo());
                    filter.run();
                    onChanged.run();
                }
            }
        });
        close.setOnAction(e -> d.close());
        HBox bs = new HBox(8, add, ed, del, close);
        bs.setAlignment(Pos.CENTER_RIGHT);
        r.getChildren().addAll(title, filters, list, bs);
        VBox.setVgrow(list, Priority.ALWAYS);
        d.setScene(new Scene(r, 760, 500));
        style(d.getScene());
        d.show();
    }
}

