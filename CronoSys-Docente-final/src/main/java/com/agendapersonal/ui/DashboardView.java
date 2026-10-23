package com.agendapersonal.ui;

import com.agendapersonal.ui.FontPreferences;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.ActividadClase;
import com.agendapersonal.domain.model.ClaseDocente;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.infrastructure.repo.ClaseDocenteDAO;
import com.agendapersonal.util.Ui;
import com.agendapersonal.ui.IconFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class DashboardView {
    private static final String[] QUOTES = {
        "Pequeños avances diarios crean grandes resultados.",
        "Enfócate en lo importante, no solo en lo urgente.",
        "Tu tiempo es tu mejor inversión.",
        "Hoy es un buen día para avanzar tu proyecto.",
        "La constancia le gana al talento cuando el talento descansa.",
        "Bloquea tu enfoque y el resto fluye.",
        "Hecho es mejor que perfecto."
    };

    private final MainController c;
    private final Stage stage;
    private final Runnable onPrepararClase;
    private final ScrollPane view;
    private TableView<Evento> agendaTable;
    private VBox taskBox;
    private Label eventCount;
    private Label taskCount;
    private Label eventosHoyValue;
    private Label eventosTotalesValue;
    private Label pendientesValue;
    private Label completadasValue;
    private Label focusTitle;
    private Label focusDetail;
    private Label clockLabel;
    private Timeline clockTimer;
    private InteractiveCalendar calendar;
    private LocalDate fechaSeleccionada = LocalDate.now();
    private Label agendaTitulo;

    public DashboardView(MainController c, Stage stage, Runnable onNewEvent,
                         Consumer<String> onSearchEvents, Runnable onNewTask, Runnable onPrepararClase) {
        this.c = c;
        this.stage = stage;
        this.onPrepararClase = onPrepararClase == null ? () -> {} : onPrepararClase;
        VBox x = new VBox(16);
        x.setPadding(new Insets(28, 36, 32, 36));
        x.getStyleClass().add("page");
        x.setFillWidth(true);

        x.getChildren().add(hero(onNewEvent));
        x.getChildren().add(searchRow(onSearchEvents));

        HBox summary = new HBox(12);
        eventosHoyValue = new Label("0");
        eventosTotalesValue = new Label("0");
        pendientesValue = new Label("0");
        completadasValue = new Label("0");
        summary.getChildren().addAll(
            statCard("Eventos hoy", eventosHoyValue, "teal", "event"),
            statCard("Eventos totales", eventosTotalesValue, "violet", "agenda"),
            statCard("Tareas pendientes", pendientesValue, "amber", "tasks"),
            statCard("Tareas completadas", completadasValue, "green", "check"));
        x.getChildren().add(summary);

        HBox cols = new HBox(14);
        calendar = new InteractiveCalendar(c, fecha -> {
            fechaSeleccionada = fecha;
            Platform.runLater(this::refreshAgenda);
        });
        VBox right = new VBox(14, agendaCard(), tasksCard(onNewTask));
        HBox.setHgrow(calendar.getView(), Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        cols.getChildren().addAll(calendar.getView(), right);
        x.getChildren().add(cols);

        this.view = new ScrollPane(x);
        this.view.setFitToWidth(true);
        this.view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.view.getStyleClass().add("page-scroll");
        c.getEventos().addListener((Observable o) -> Platform.runLater(this::refresh));
        c.getTareas().addListener((Observable o) -> Platform.runLater(this::refresh));
        refresh();
    }

    // ---------- héroe ----------
    private VBox hero(Runnable onNewEvent) {
        VBox hero = new VBox(8);
        hero.setPadding(new Insets(26, 30, 26, 30));
        hero.getStyleClass().add("dashboard-hero");
        Label hi = new Label(com.agendapersonal.util.Perfil.saludo(LocalTime.now().getHour(), com.agendapersonal.util.Perfil.nombre()));
        com.agendapersonal.util.Perfil.revisionProperty().addListener((o, a, b) ->
            hi.setText(com.agendapersonal.util.Perfil.saludo(LocalTime.now().getHour(), com.agendapersonal.util.Perfil.nombre())));
        FontPreferences.bind(hi, FontWeight.BOLD, 30);
        hi.setStyle("-fx-text-fill:-text;");
        Label quote = new Label(QUOTES[LocalDate.now().getDayOfYear() % QUOTES.length]);
        quote.setWrapText(true);
        quote.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");

        clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill:-text;-fx-padding:0;");
        Runnable updateClock = () -> clockLabel.setText(RelojPrefs.formatearAhora());
        Runnable aplicarPrefs = () -> {
            FontPreferences.bind(clockLabel, FontWeight.BOLD, RelojPrefs.tamano().px());
            clockLabel.setVisible(RelojPrefs.visible());
            clockLabel.setManaged(RelojPrefs.visible());
            updateClock.run();
        };
        aplicarPrefs.run();
        RelojPrefs.revisionProperty().addListener((o, a, b) -> aplicarPrefs.run());
        clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock.run()));
        clockTimer.setCycleCount(Timeline.INDEFINITE);
        clockTimer.play();

        Label dateText = new Label(Ui.formatDate(LocalDate.now()));
        dateText.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");
        HBox date = new HBox(8, IconFactory.icon("calendar", 18, "tint-muted"), dateText);
        date.setAlignment(Pos.CENTER_LEFT);

        Button nuevo = Ui.actionButton("add", "Nuevo evento");
        Ui.stylePrimary(nuevo);
        nuevo.setOnAction(e -> onNewEvent.run());
        HBox btns = new HBox(10, nuevo);
        if (com.agendapersonal.util.Perfil.esDocente()) {
            Button preparar = Ui.actionButton("play", "Preparar clase");
            preparar.setOnAction(e -> onPrepararClase.run());
            btns.getChildren().add(preparar);
        }

        VBox left = new VBox(3, hi, quote, clockLabel, date, btns);

        // Tarjeta "enfoque ahora" sobre el héroe
        VBox focus = new VBox(4);
        focus.setPadding(new Insets(16));
        focus.getStyleClass().add("dashboard-focus");
        focus.setPrefWidth(290);
        focus.setMinHeight(170);
        Label fh = new Label("ENFOQUE AHORA");
        fh.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-font-weight:bold;");
        HBox focusHeader = new HBox(8, IconFactory.icon("focus", 18, "tint-accent"), fh);
        focusHeader.setAlignment(Pos.CENTER_LEFT);
        focusTitle = new Label("—");
        FontPreferences.bind(focusTitle, FontWeight.BOLD, 16);
        focusTitle.setStyle("-fx-text-fill:-text;");
        focusTitle.setWrapText(true);
        focusDetail = new Label("");
        focusDetail.setStyle("-fx-text-fill:-muted;-fx-font-size:12;");
        focusDetail.setWrapText(true);
        focus.getChildren().addAll(focusHeader, focusTitle, focusDetail);

        HBox row = new HBox(16, left, spacer(), focus);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        hero.getChildren().add(row);
        return hero;
    }

    private HBox searchRow(Consumer<String> onSearchEvents) {
        TextField quickSearch = new TextField();
        quickSearch.setPromptText("Buscar eventos por título o descripción...");
        HBox.setHgrow(quickSearch, Priority.ALWAYS);
        Button searchBtn = new Button("Buscar");
        Ui.stylePrimary(searchBtn);
        Runnable go = () -> onSearchEvents.accept(quickSearch.getText().trim());
        searchBtn.setOnAction(e -> go.run());
        quickSearch.setOnAction(e -> go.run());
        HBox h = new HBox(8, quickSearch, searchBtn);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox statCard(String label, Label valueLabel, String tone, String icon) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        HBox top = new HBox(8, l, spacer(), Ui.iconChip(icon, tone, 34, 18));
        top.setAlignment(Pos.CENTER_LEFT);
        valueLabel.getStyleClass().add("stat-value");
        VBox b = new VBox(10, top, valueLabel);
        b.getStyleClass().addAll("card", "stat-card");
        b.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ---------- tarjetas agenda / tareas ----------
    private VBox agendaCard() {
        VBox b = Ui.card();
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        agendaTitulo = new Label("Agenda de hoy");
        FontPreferences.bind(agendaTitulo, FontWeight.BOLD, 17);
        agendaTitulo.setStyle("-fx-text-fill:-text;");
        eventCount = new Label();
        eventCount.setStyle("-fx-text-fill:-muted;");
        Region sp = spacer();
        Button hoyBtn = new Button("Hoy");
        hoyBtn.setOnAction(e -> {
            fechaSeleccionada = LocalDate.now();
            calendar.setSelected(fechaSeleccionada);
            refreshAgenda();
        });
        h.getChildren().addAll(agendaTitulo, eventCount, sp, hoyBtn);
        agendaTable = buildAgendaTable();
        b.getChildren().addAll(h, agendaTable);
        return b;
    }

    private VBox tasksCard(Runnable onNewTask) {
        VBox b = Ui.card();
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label tt = new Label("Tareas");
        FontPreferences.bind(tt, FontWeight.BOLD, 17);
        tt.setStyle("-fx-text-fill:-text;");
        taskCount = new Label();
        taskCount.setStyle("-fx-text-fill:-muted;");
        Region sp = spacer();
        Button addTask = Ui.actionButton("add", "Nueva");
        Ui.stylePrimary(addTask);
        addTask.setOnAction(e -> onNewTask.run());
        h.getChildren().addAll(tt, taskCount, sp, addTask);
        taskBox = new VBox(6);
        b.getChildren().addAll(h, new Separator(), taskBox);
        return b;
    }

    public ScrollPane getView() { return view; }

    public void refresh() {
        if (agendaTable == null) return;
        if (calendar != null) calendar.refreshMarks();
        refreshAgenda();
        refreshSummary();
        refreshFocus();
    }

    private void refreshAgenda() {
        LocalDate hoy = LocalDate.now();
        LocalDate sel = fechaSeleccionada != null ? fechaSeleccionada : hoy;
        boolean esHoy = sel.equals(hoy);
        if (agendaTitulo != null)
            agendaTitulo.setText(esHoy ? "Agenda de hoy"
                : "Agenda: " + sel.getDayOfMonth() + "/" + sel.getMonthValue() + "/" + sel.getYear());
        if (esHoy) {
            List<Evento> proximos = c.getEventos().stream()
                .filter(e -> e.getFecha() != null && !e.getFecha().isBefore(hoy))
                .limit(8).toList();
            agendaTable.getItems().setAll(proximos);
            if (eventCount != null) eventCount.setText("(" + proximos.size() + ")");
        } else {
            List<Evento> delDia = c.eventosDelDia(sel);
            agendaTable.getItems().setAll(delDia);
            if (eventCount != null) eventCount.setText("(" + delDia.size() + ")");
        }

        taskBox.getChildren().clear();
        List<Tarea> ts = c.getTareas();
        long done = ts.stream().filter(t -> "Completada".equals(t.getEstado())).count();
        if (ts.isEmpty()) {
            Label l = new Label("Sin tareas. Crea la primera con + Nueva.");
            l.setStyle("-fx-text-fill:-muted;");
            taskBox.getChildren().add(l);
        } else for (Tarea t : ts.stream().limit(4).toList()) taskBox.getChildren().add(taskRow(t));
        if (taskCount != null) taskCount.setText("(" + done + "/" + ts.size() + ")");
        refreshSummary();
    }

    private void refreshSummary() {
        List<Evento> es = c.eventosDelDia(LocalDate.now());
        List<Tarea> ts = c.getTareas();
        long done = ts.stream().filter(t -> "Completada".equals(t.getEstado())).count();
        if (eventosHoyValue != null) eventosHoyValue.setText(String.valueOf(es.size()));
        if (eventosTotalesValue != null) eventosTotalesValue.setText(String.valueOf(c.getEventos().size()));
        if (pendientesValue != null) pendientesValue.setText(String.valueOf(ts.size() - done));
        if (completadasValue != null) completadasValue.setText(String.valueOf(done));
    }

    private void refreshFocus() {
        if (com.agendapersonal.util.Perfil.esDocente()) { refrescarEnfoqueDocente(); return; }
        LocalTime now = LocalTime.now();
        Evento next = null;
        long bestMin = Long.MAX_VALUE;
        for (Evento e : c.eventosDelDia(LocalDate.now())) {
            try {
                long m = java.time.Duration.between(now, LocalTime.parse(e.getHoraInicio())).toMinutes();
                if (m >= -30 && m < bestMin) { bestMin = m; next = e; }
            } catch (Exception ignored) {}
        }
        if (next == null) {
            var upcoming = c.getEventos().stream()
                .filter(e -> e.getFecha() != null && e.getFecha().isAfter(LocalDate.now()))
                .sorted((a, b2) -> a.getFecha().compareTo(b2.getFecha())).findFirst();
            if (upcoming.isPresent()) {
                next = upcoming.get();
                bestMin = -1;
            }
        }
        if (focusTitle == null) return;
        if (next == null) {
            focusTitle.setText("Día despejado");
            focusDetail.setText("Sin eventos próximos. Perfecto para avanzar proyectos.");
        } else if (bestMin == -1) {
            focusTitle.setText(next.getTitulo());
            focusDetail.setText(next.getFecha() + " • " + next.getHoraInicio());
        } else if (bestMin <= 0) {
            focusTitle.setText(next.getTitulo());
            focusDetail.setText("¡Ahora mismo! " + next.getHoraInicio() + "-" + next.getHoraFin());
        } else if (bestMin < 60) {
            focusTitle.setText(next.getTitulo());
            focusDetail.setText("En " + bestMin + " min • " + next.getHoraInicio());
        } else {
            focusTitle.setText(next.getTitulo());
            focusDetail.setText("En " + (bestMin / 60) + " h " + (bestMin % 60) + " min • " + next.getHoraInicio());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<Evento> buildAgendaTable() {
        TableView<Evento> t = new TableView<>();
        t.setPrefHeight(250);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label("No hay eventos para mostrar."));

        TableColumn<Evento, Void> colNum = new TableColumn<>("#");
        colNum.setPrefWidth(40);
        colNum.setMaxWidth(50);
        colNum.setSortable(false);
        colNum.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        TableColumn<Evento, String> colHora = new TableColumn<>("Hora");
        colHora.setPrefWidth(130);
        colHora.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getHoraInicio() + " - " + cd.getValue().getHoraFin()));

        TableColumn<Evento, String> colNombre = new TableColumn<>("Evento");
        colNombre.setPrefWidth(220);
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitulo()));

        TableColumn<Evento, Void> colAcc = new TableColumn<>("Acciones");
        colAcc.setPrefWidth(150);
        colAcc.setSortable(false);
        colAcc.setCellFactory(col -> new TableCell<>() {
            private final Button edit = iconBtn("edit", "Editar");
            private final Button reprogram = iconBtn("reschedule", "Reprogramar");
            private final Button del = iconBtn("delete", "Eliminar");
            private final HBox box = new HBox(6, edit, reprogram, del);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                edit.setOnAction(e -> {
                    Evento ev = rowAt(getTableView(), getIndex());
                    if (ev != null) EventViews.form(stage, ev, c, () -> refresh());
                });
                reprogram.setOnAction(e -> {
                    Evento ev = rowAt(getTableView(), getIndex());
                    if (ev != null) EventViews.reprogramar(stage, ev, c, () -> refresh());
                });
                del.setOnAction(e -> {
                    Evento ev = rowAt(getTableView(), getIndex());
                    if (ev == null) return;
                    Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + ev.getTitulo() + "\"?");
                    if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && c.eliminarEvento(ev.getId()))
                        Notifier.deleted("Evento eliminado", ev.getTitulo());
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(colNum, colHora, colNombre, colAcc);
        return t;
    }

    private Button iconBtn(String icon, String tip) {
        Button b = new Button();
        b.setGraphic(IconFactory.image(icon, 16));
        b.setTooltip(new Tooltip(tip));
        b.setAccessibleText(tip);
        b.getStyleClass().add("icon-action-button");
        return b;
    }

    private static Evento rowAt(TableView<Evento> t, int i) {
        if (t == null || i < 0 || i >= t.getItems().size()) return null;
        return t.getItems().get(i);
    }

    private HBox taskRow(Tarea t) {
        CheckBox cb = new CheckBox(t.getTitulo());
        cb.setSelected("Completada".equals(t.getEstado()));
        cb.setOnAction(e -> {
            var ventana = cb.getScene() == null ? null : cb.getScene().getWindow();
            var res = c.cambiarEstadoTarea(t, cb.isSelected()
                ? com.agendapersonal.model.EstadoTarea.COMPLETADA : com.agendapersonal.model.EstadoTarea.PENDIENTE);
            res.ifPresent(r -> Toast.mostrar(ventana, c.mensajeProgreso(r)));
            refresh();
        });
        HBox r = new HBox(cb);
        r.setPadding(new Insets(6));
        r.setStyle("-fx-background-color:-surface2;-fx-background-radius:8;");
        return r;
    }

    /** Enfoque ahora para docentes: la próxima clase de su planeación. */
    private void refrescarEnfoqueDocente() {
        LocalDate hoy = LocalDate.now();
        ActividadClase proxima = null;
        ClaseDocente clase = null;
        for (ClaseDocente cl : new ClaseDocenteDAO().listarClases()) {
            for (ActividadClase a : new ClaseDocenteDAO().listarActividades(cl.id())) {
                if (a.fecha() == null || a.fecha().isBefore(hoy)) continue;
                if (proxima == null || a.fecha().isBefore(proxima.fecha())) { proxima = a; clase = cl; }
            }
        }
        if (focusTitle == null) return;
        if (proxima == null) {
            focusTitle.setText("Sin clases aún");
            focusDetail.setText("Crea tu primer curso y su planeación en el Espacio Docente.");
        } else {
            focusTitle.setText("Próxima clase: " + proxima.titulo());
            focusDetail.setText((clase == null ? "" : clase.nombre() + " · ") + proxima.fecha());
        }
    }
}

