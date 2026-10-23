package com.agendapersonal.ui;

import com.agendapersonal.ui.FontPreferences;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.Calendario;
import com.agendapersonal.infrastructure.repo.CalendarioDAO;
import com.agendapersonal.model.Evento;
import com.agendapersonal.service.AgendaService;
import com.agendapersonal.service.ConflictoHorario;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class EventViews {
    private EventViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void style(Scene sc) {
        com.agendapersonal.util.Ui.decorarDialogo(sc);
    }

    public static void form(Stage owner, Evento old, MainController c, Runnable onSaved) {
        formEnFecha(owner, old, null, c, onSaved);
    }

    /** Nuevo evento con fecha predefinida (clic en un día del calendario). */
    public static void formEnFecha(Stage owner, Evento old, LocalDate preset, MainController c, Runnable onSaved) {
        formEnFechaHora(owner, old, preset, null, null, c, onSaved);
    }

    /**
     * Nuevo evento con fecha, hora de inicio y categoría predefinidas (clic en la cuadrícula horaria
     * del calendario, o "Crear → Trabajo"). Los valores {@code null} usan los predeterminados.
     */
    public static void formEnFechaHora(Stage owner, Evento old, LocalDate preset, String horaInicio,
                                       String categoria, MainController c, Runnable onSaved) {
        Stage d = new Stage();
        if (owner != null) { d.initOwner(owner); d.initModality(Modality.WINDOW_MODAL); }
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(22));
        r.setPrefWidth(430);
        TextField title = new TextField(old == null ? "" : old.getTitulo());
        DatePicker date = new DatePicker(old == null ? (preset != null ? preset : LocalDate.now()) : old.getFecha());
        TextField hi = new TextField(old == null ? (horaInicio != null ? horaInicio : "09:00") : old.getHoraInicio());
        TextField hf = new TextField(old == null ? (horaInicio != null ? masUnaHora(horaInicio) : "10:00") : old.getHoraFin());
        TextArea desc = new TextArea(old == null ? "" : old.getDescripcion());
        ComboBox<String> cat = new ComboBox<>(), pri = new ComboBox<>();
        cat.getItems().addAll("Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        pri.getItems().addAll("Baja", "Media", "Alta");
        cat.setValue(old == null ? (categoria != null ? categoria : "Personal") : old.getCategoria());
        pri.setValue(old == null ? "Media" : old.getPrioridad());
        ComboBox<String> cal = new ComboBox<>();
        List<Calendario> cals = new CalendarioDAO().listar();
        for (var k : cals) cal.getItems().add(k.getNombre());
        int calSel = 0;
        if (old != null && old.getCalendarioId() != null)
            for (int i = 0; i < cals.size(); i++)
                if (cals.get(i).getId() == old.getCalendarioId()) calSel = i;
        if (!cals.isEmpty()) cal.setValue(cals.get(calSel).getNombre());
        Button save = new Button(old == null ? "Guardar" : "Actualizar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(e -> {
            if (title.getText().isBlank() || date.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            int ci = cal.getSelectionModel().getSelectedIndex();
            Integer calId = (ci >= 0 && ci < cals.size()) ? cals.get(ci).getId() : null;
            Evento n = new Evento(old == null ? 0 : old.getId(), title.getText().trim(), date.getValue(),
                hi.getText().trim(), hf.getText().trim(), desc.getText().trim(), cat.getValue(), pri.getValue(), calId);
            if (guardarValidado(n, old == null, c)) { d.close(); onSaved.run(); }
        });
        cancel.setOnAction(e -> d.close());
        r.getChildren().addAll(new Label(old == null ? "Nuevo evento" : "Editar evento"), new Label("Título"), title,
            new Label("Fecha"), date, new Label("Hora inicio / fin"), new HBox(8, hi, hf),
            new Label("Descripción"), desc, new Label("Categoría"), cat, new Label("Prioridad"), pri,
            new Label("Calendario"), cal,
            new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        style(d.getScene());
        d.showAndWait();
    }

    /** "14:00" → "15:00"; si ya es la hora 23 devuelve "23:59" (el evento no cruza la medianoche). */
    private static String masUnaHora(String hi) {
        if (!com.agendapersonal.model.Horas.esValida(hi)) return "10:00";
        var t = com.agendapersonal.model.Horas.parsear(hi);
        return t.getHour() >= 23 ? "23:59" : t.plusHours(1).format(com.agendapersonal.model.Horas.SALIDA);
    }

    /**
     * Valida el evento, detecta cruces con la agenda y guarda.
     * Si hay solapamiento, informa los minutos y deja elegir: ajustar (volver) o forzar.
     * @return true si el evento quedó guardado.
     */
    private static boolean guardarValidado(Evento n, boolean esNuevo, MainController c) {
        var error = AgendaService.validar(n);
        if (error.isPresent()) {
            new Alert(Alert.AlertType.WARNING, error.get()).showAndWait();
            return false;
        }
        List<ConflictoHorario> conflictos = c.conflictosPara(n);
        if (!conflictos.isEmpty()) {
            StringBuilder sb = new StringBuilder("Este evento se cruza con:\n");
            for (ConflictoHorario k : conflictos) sb.append("  • ").append(k.descripcion()).append('\n');
            c.huecoLibre(n.getFecha(), AgendaService.duracionMinutos(n)).ifPresent(h ->
                sb.append("\nHueco libre sugerido ese día: ")
                  .append(AgendaService.hora(h[0])).append(" – ").append(AgendaService.hora(h[1])));
            ButtonType ajustar = new ButtonType("Ajustar horario", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType forzar = new ButtonType("Guardar de todos modos", ButtonBar.ButtonData.OK_DONE);
            Alert al = new Alert(Alert.AlertType.WARNING, sb.toString(), ajustar, forzar);
            al.setTitle("Conflicto de horario");
            al.setHeaderText("Cruce de horarios detectado");
            if (al.showAndWait().orElse(ajustar) != forzar) return false;
        }
        boolean ok = esNuevo ? c.guardarEvento(n) : c.actualizarEvento(n);
        if (!ok) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar.").showAndWait();
            return false;
        }
        Toast.mostrarGlobal((esNuevo ? "Evento guardado" : "Evento actualizado")
            + " · duración " + AgendaService.formatearMinutos(AgendaService.duracionMinutos(n)));
        return true;
    }

    /** Reprogramar: cambia solo fecha y horas del evento. */
    public static void reprogramar(Stage owner, Evento e, MainController c, Runnable onSaved) {
        Stage d = new Stage();
        if (owner != null) { d.initOwner(owner); d.initModality(Modality.WINDOW_MODAL); }
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(340);
        Label t = new Label("Reprogramar: " + e.getTitulo());
        FontPreferences.bind(t, FontWeight.BOLD, 15);
        DatePicker date = new DatePicker(e.getFecha());
        TextField hi = new TextField(e.getHoraInicio());
        TextField hf = new TextField(e.getHoraFin());
        hi.setPromptText("Hora inicio (HH:mm)");
        hf.setPromptText("Hora fin (HH:mm)");
        Button save = new Button("Guardar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(ev -> {
            if (date.getValue() == null || hi.getText().isBlank() || hf.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Complete fecha y horas.").showAndWait();
                return;
            }
            Evento n = new Evento(e.getId(), e.getTitulo(), date.getValue(),
                hi.getText().trim(), hf.getText().trim(), e.getDescripcion(), e.getCategoria(), e.getPrioridad(),
                e.getCalendarioId());
            if (guardarValidado(n, false, c)) { d.close(); onSaved.run(); }
        });
        cancel.setOnAction(ev -> d.close());
        r.getChildren().addAll(t, new Label("Nueva fecha"), date,
            new Label("Hora inicio / fin"), new HBox(8, hi, hf), new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        style(d.getScene());
        d.showAndWait();
    }

    public static void manage(String initialSearch, MainController c, Stage formOwner, Runnable onChanged) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(760);
        Label title = new Label("Gestión de eventos");
        FontPreferences.bind(title, FontWeight.BOLD, 20);
        TextField search = new TextField(initialSearch);
        search.setPromptText("Buscar por título o descripción...");
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll("Todas", "Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        cat.setValue("Todas");
        ComboBox<String> pri = new ComboBox<>();
        pri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        pri.setValue("Todas");
        ComboBox<String> calF = new ComboBox<>();
        calF.getItems().add("Todas");
        List<Calendario> calsF = new CalendarioDAO().listar();
        for (var k : calsF) calF.getItems().add(k.getNombre());
        calF.setValue("Todas");
        HBox filters = new HBox(8, search, cat, pri, calF);
        HBox.setHgrow(search, Priority.ALWAYS);
        ListView<Evento> list = new ListView<>();
        Runnable filter = () -> {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            Integer selCal = null;
            if (!"Todas".equals(calF.getValue()))
                for (var k : calsF) if (k.getNombre().equals(calF.getValue())) selCal = k.getId();
            final Integer fCal = selCal;
            list.getItems().setAll(c.getEventos().stream()
                .filter(x -> q.isBlank() || x.getTitulo().toLowerCase(Locale.ROOT).contains(q)
                    || (x.getDescripcion() != null && x.getDescripcion().toLowerCase(Locale.ROOT).contains(q)))
                .filter(x -> "Todas".equals(cat.getValue()) || x.getCategoria().equals(cat.getValue()))
                .filter(x -> "Todas".equals(pri.getValue()) || x.getPrioridad().equals(pri.getValue()))
                .filter(x -> fCal == null || (x.getCalendarioId() != null && x.getCalendarioId().intValue() == fCal.intValue())).toList());
        };
        search.textProperty().addListener((o, old, n) -> filter.run());
        cat.setOnAction(e -> filter.run());
        pri.setOnAction(e -> filter.run());
        calF.setOnAction(e -> filter.run());
        filter.run();
        // En vivo: si llegan datos nuevos (polling u otra ventana), re-filtra solo
        javafx.collections.ListChangeListener<Evento> live =
            ch -> javafx.application.Platform.runLater(filter::run);
        c.getEventos().addListener(live);
        d.setOnHidden(e -> c.getEventos().removeListener(live));
        Button ed = new Button("Editar"), del = new Button("Eliminar"), close = new Button("Cerrar");
        ed.setOnAction(e -> {
            Evento x = list.getSelectionModel().getSelectedItem();
            if (x != null) { form(d, x, c, () -> { filter.run(); onChanged.run(); }); }
        });
        del.setOnAction(e -> {
            Evento x = list.getSelectionModel().getSelectedItem();
            if (x != null) {
                Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + x.getTitulo() + "\"?");
                if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (c.eliminarEvento(x.getId())) Notifier.deleted("Evento eliminado", x.getTitulo());
                    filter.run();
                    onChanged.run();
                }
            }
        });
        close.setOnAction(e -> d.close());
        HBox bs = new HBox(8, ed, del, close);
        bs.setAlignment(Pos.CENTER_RIGHT);
        r.getChildren().addAll(title, filters, list, bs);
        VBox.setVgrow(list, Priority.ALWAYS);
        d.setScene(new Scene(r, 760, 500));
        style(d.getScene());
        d.show();
    }
}

