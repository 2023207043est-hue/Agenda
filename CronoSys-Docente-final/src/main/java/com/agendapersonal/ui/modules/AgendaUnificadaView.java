package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.infrastructure.db.ConfigDAO;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Horas;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.AgendaDatos;
import com.agendapersonal.service.CalendarioUnificadoService;
import com.agendapersonal.service.CalendarioUnificadoService.Datos;
import com.agendapersonal.service.Entrada;
import com.agendapersonal.service.TipoEntrada;
import com.agendapersonal.ui.EventViews;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.TaskViews;
import com.agendapersonal.ui.Toast;
import com.agendapersonal.ui.notify.CentroAvisos;
import com.agendapersonal.ui.notify.NotificacionEscritorio;
import com.agendapersonal.util.Ui;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Agenda Unificada: un panel estilo Google Calendar que junta en un solo lugar
 * eventos, trabajos, tareas, hábitos y cumpleaños, y permite mostrarlos u ocultarlos por segmento.
 *
 * Interacción:
 *  - Clic en un hueco de la cuadrícula → nuevo evento a esa hora.
 *  - Evento/trabajo: clic para editar.  Tarea: clic marca/desmarca, clic derecho edita.
 *  - Hábito: clic marca/desmarca ese día.  Cumpleaños: clic muestra los datos del contacto.
 */
public class AgendaUnificadaView {
    private enum Vista {
        DIA("Día"), SEMANA("Semana"), MES("Mes"), AGENDA("Agenda");
        final String etiqueta;
        Vista(String e) { etiqueta = e; }
    }

    private static final double PX_HORA = 60, GUTTER = 58;
    private static final DayOfWeek PRIMER_DIA = DayOfWeek.SUNDAY;   // como en tu Google Calendar
    private static final Locale ES = Locale.of("es", "PE");
    private static final String MUTED = "-muted", TEXTO = "-text", LINEA = "-bord", PANEL = "-surface";

    private final MainController c;
    private final Stage stage;
    private final ConfigDAO cfg = new ConfigDAO();
    private final HabitoDAO habitoDAO = new HabitoDAO();

    private final BorderPane view = new BorderPane();
    private final StackPane centro = new StackPane();
    private final Label rango = new Label();
    private final VBox mini = new VBox(6);
    private final ToggleGroup grupoVista = new ToggleGroup();
    private final Map<Vista, ToggleButton> botonesVista = new EnumMap<>(Vista.class);
    private final Map<TipoEntrada, Label> contadores = new EnumMap<>(TipoEntrada.class);
    private final Set<TipoEntrada> visibles = EnumSet.allOf(TipoEntrada.class);
    private final List<ColumnaDia> columnas = new ArrayList<>();

    private LocalDate cursor = LocalDate.now();
    private YearMonth miniMes = YearMonth.now();
    private Vista vista = Vista.SEMANA;
    private Datos datos = Datos.vacio();
    private String firmaDatos = "";
    private double vvalue = -1;      // posición vertical recordada de la cuadrícula horaria

    public AgendaUnificadaView(MainController c, Stage stage) {
        this.c = c;
        this.stage = stage;
        view.setStyle("-fx-background-color:-bg;");
        view.setTop(barraSuperior());
        view.setLeft(panelIzquierdo());
        view.setCenter(centro);
        BorderPane.setMargin(centro, new Insets(0, 14, 14, 0));

        // se actualiza sola cuando cambian eventos/tareas (incluye el refresco cada 15 s de la BD)
        c.getEventos().addListener((Observable o) -> Platform.runLater(() -> refrescar(false)));
        c.getTareas().addListener((Observable o) -> Platform.runLater(() -> refrescar(false)));
        Timeline cadaMinuto = new Timeline(new KeyFrame(Duration.seconds(60), e -> actualizarAhora()));
        cadaMinuto.setCycleCount(Animation.INDEFINITE);
        cadaMinuto.play();

        botonesVista.get(vista).setSelected(true);
        refrescar(true);
    }

    public BorderPane getView() { return view; }

    // =====================================================================================
    //  Barra superior
    // =====================================================================================

    private HBox barraSuperior() {
        Button hoy = new Button("Hoy");
        hoy.setStyle(estiloContorno());
        hoy.setOnAction(e -> irA(LocalDate.now()));
        Button prev = flecha("arrow-left");
        Button next = flecha("arrow-right");
        prev.setOnAction(e -> mover(-1));
        next.setOnAction(e -> mover(1));
        rango.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:22;");

        HBox seg = new HBox(0);
        seg.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;-fx-padding:2;");
        for (Vista v : Vista.values()) {
            ToggleButton b = new ToggleButton(v.etiqueta);
            b.setToggleGroup(grupoVista);
            estiloSeg(b, false);
            b.selectedProperty().addListener((o, a, sel) -> estiloSeg(b, sel));
            b.setOnAction(e -> {
                b.setSelected(true);                       // no permitir "ninguna" vista
                if (vista != v) { vista = v; refrescar(true); }
            });
            botonesVista.put(v, b);
            seg.getChildren().add(b);
        }
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, hoy, prev, next, rango, sp, seg);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 10, 16));
        return bar;
    }

    private static String estiloContorno() {
        return "-fx-background-color:transparent;-fx-border-color:" + LINEA + ";-fx-border-radius:18;"
            + "-fx-background-radius:18;-fx-text-fill:" + TEXTO + ";-fx-padding:6 16;-fx-cursor:hand;";
    }

    private static Button flecha(String icon) {
        Button b = new Button();
        b.setGraphic(IconFactory.image(icon, 16));
        b.setAccessibleText(icon.equals("arrow-left") ? "Anterior" : "Siguiente");
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:" + TEXTO + ";-fx-cursor:hand;-fx-padding:6 8;");
        return b;
    }

    private static MenuItem menuItem(String text, String icon) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(IconFactory.image(icon, 16));
        return item;
    }

    private static void estiloSeg(ToggleButton b, boolean sel) {
        b.setStyle("-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;" + (sel
            ? "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
            : "-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";"));
    }

    // =====================================================================================
    //  Panel izquierdo: Crear + mini calendario + segmentos + notificaciones
    // =====================================================================================

    private Node panelIzquierdo() {
        VBox p = new VBox(16);
        p.setPadding(new Insets(6, 14, 14, 16));
        p.setPrefWidth(280);

        MenuButton crear = new MenuButton("Crear");
        crear.setGraphic(IconFactory.image("add", 17));
        crear.setStyle("-fx-background-color:-surface2;-fx-text-fill:" + TEXTO + ";-fx-background-radius:24;"
            + "-fx-border-color:" + LINEA + ";-fx-border-radius:24;-fx-padding:10 22;-fx-font-weight:bold;-fx-cursor:hand;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),8,0,0,2);");
        MenuItem miEvento = menuItem("Evento", "event");
        MenuItem miTrabajo = menuItem("Trabajo", "work");
        MenuItem miTarea = menuItem("Tarea", "tasks");
        MenuItem miHabito = menuItem("Hábito", "habits");
        MenuItem miCumple = menuItem("Cumpleaños", "birthday");
        miEvento.setOnAction(e -> EventViews.formEnFechaHora(stage, null, cursor, null, null, c, () -> refrescar(true)));
        miTrabajo.setOnAction(e -> EventViews.formEnFechaHora(stage, null, cursor, null, "Trabajo", c, () -> refrescar(true)));
        miTarea.setOnAction(e -> TaskViews.form(null, c, () -> refrescar(true)));
        miHabito.setOnAction(e -> DialogosAgenda.nuevoHabito(stage, () -> refrescar(true)));
        miCumple.setOnAction(e -> DialogosAgenda.nuevoCumple(stage, () -> refrescar(true)));
        crear.getItems().addAll(miEvento, miTrabajo, miTarea, miHabito, miCumple);

        p.getChildren().addAll(crear, mini, titulo("Mis calendarios"));
        for (TipoEntrada t : TipoEntrada.values()) p.getChildren().add(filtro(t));
        p.getChildren().addAll(titulo("Notificaciones"), panelNotificaciones());

        ScrollPane sp = new ScrollPane(p);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefWidth(300);
        sp.setMinWidth(300);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        return sp;
    }

    private static Label titulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        VBox.setMargin(l, new Insets(4, 0, -8, 4));
        return l;
    }

    /** Fila "casilla de color + nombre + contador" que muestra/oculta un segmento. */
    private HBox filtro(TipoEntrada t) {
        Region caja = new Region();
        caja.setMinSize(16, 16);
        caja.setMaxSize(16, 16);
        Label nombre = new Label(t.etiqueta());
        nombre.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:13.5;");
        Label cuenta = new Label();
        cuenta.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        contadores.put(t, cuenta);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox fila = new HBox(10, caja, nombre, sp, cuenta);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(6, 10, 6, 10));
        fila.setCursor(Cursor.HAND);
        Runnable pintar = () -> caja.setStyle(visibles.contains(t)
            ? "-fx-background-color:" + t.color() + ";-fx-background-radius:4;"
            : "-fx-background-color:transparent;-fx-border-color:" + t.color() + ";-fx-border-width:2;-fx-border-radius:4;");
        pintar.run();
        fila.setOnMouseEntered(e -> fila.setStyle("-fx-background-color:-accent-soft;-fx-background-radius:8;"));
        fila.setOnMouseExited(e -> fila.setStyle(""));
        fila.setOnMouseClicked(e -> {
            if (!visibles.remove(t)) visibles.add(t);
            pintar.run();
            refrescar(true);
        });
        Tooltip.install(fila, new Tooltip("Mostrar / ocultar " + t.etiqueta().toLowerCase(ES)));
        return fila;
    }

    private VBox panelNotificaciones() {
        // Los avisos de escritorio salen solo en el momento en que empieza un evento (sin antelación).
        CheckBox eventos = new CheckBox("Avisar cuando empieza un evento");
        eventos.setSelected(!"0".equals(cfg.get(CentroAvisos.CFG_EVENTOS, "1")));
        eventos.setOnAction(e -> cfg.set(CentroAvisos.CFG_EVENTOS, eventos.isSelected() ? "1" : "0"));
        CheckBox resumen = new CheckBox("Resumen al iniciar Compás");
        resumen.setSelected("1".equals(cfg.get(CentroAvisos.CFG_RESUMEN, "0")));
        resumen.setOnAction(e -> cfg.set(CentroAvisos.CFG_RESUMEN, resumen.isSelected() ? "1" : "0"));

        Button probar = new Button("Probar notificación");
        probar.setGraphic(IconFactory.image("notification", 16));
        probar.setOnAction(e -> NotificacionEscritorio.mostrar(TipoEntrada.EVENTO, "Notificación de prueba",
            "Así verás tus recordatorios en la pantalla, incluso con Compás minimizado.", 8));

        Label nota = new Label("Al cerrar la ventana, Compás sigue avisando desde la bandeja del sistema. Más opciones en Configuración.");
        nota.setWrapText(true);
        nota.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        VBox v = new VBox(10, eventos, resumen, probar, nota);
        v.setPadding(new Insets(0, 0, 0, 4));
        return v;
    }

    // ---------- mini calendario ----------

    private void reconstruirMini() {
        mini.getChildren().clear();
        Label t = new Label(cap(miniMes.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + miniMes.getYear());
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button ant = flecha("arrow-left"), sig = flecha("arrow-right");
        ant.setOnAction(e -> { miniMes = miniMes.minusMonths(1); reconstruirMini(); });
        sig.setOnAction(e -> { miniMes = miniMes.plusMonths(1); reconstruirMini(); });
        HBox cab = new HBox(2, t, sp, ant, sig);
        cab.setAlignment(Pos.CENTER_LEFT);

        GridPane g = new GridPane();
        g.setHgap(2);
        g.setVgap(2);
        String[] ds = {"D", "L", "M", "X", "J", "V", "S"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(ds[i]);
            l.setMinWidth(30);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            g.add(l, i, 0);
        }
        LocalDate primero = miniMes.atDay(1).with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 42; i++) {
            LocalDate d = primero.plusDays(i);
            boolean fuera = !YearMonth.from(d).equals(miniMes);
            Button b = new Button(String.valueOf(d.getDayOfMonth()));
            b.setMinSize(30, 30);
            b.setPrefSize(30, 30);
            b.setMaxSize(30, 30);
            String bg, fg, extra = "";
            if (d.equals(hoy)) { bg = "-accent"; fg = "-on-accent"; extra = "-fx-font-weight:bold;"; }
            else if (d.equals(cursor)) { bg = "-accent-soft"; fg = "-text"; extra = "-fx-font-weight:bold;"; }
            else if (CalendarioUnificadoService.hayAlgo(d, datos, visibles)) { bg = "-surface2"; fg = "-text"; }
            else { bg = "transparent"; fg = fuera ? "-muted" : "-text2"; }
            b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-background-radius:15;"
                + "-fx-padding:0;-fx-font-size:12;-fx-cursor:hand;" + extra);
            b.setOnAction(e -> irA(d));
            g.add(b, i % 7, 1 + i / 7);
        }
        mini.getChildren().addAll(cab, g);
    }

    // =====================================================================================
    //  Navegación y refresco
    // =====================================================================================

    private void mover(int dir) {
        cursor = switch (vista) {
            case DIA -> cursor.plusDays(dir);
            case SEMANA -> cursor.plusWeeks(dir);
            case MES, AGENDA -> cursor.plusMonths(dir);
        };
        miniMes = YearMonth.from(cursor);
        refrescar(true);
    }

    private void irA(LocalDate d) {
        cursor = d;
        miniMes = YearMonth.from(d);
        refrescar(true);
    }

    private void irADia(LocalDate d) {
        cursor = d;
        miniMes = YearMonth.from(d);
        vista = Vista.DIA;
        botonesVista.get(Vista.DIA).setSelected(true);
        refrescar(true);
    }

    /** @param forzar true si cambió la vista/fecha/filtros; false cuando solo puede haber cambiado la BD. */
    private void refrescar(boolean forzar) {
        LocalDate desde = cursor.minusMonths(1).withDayOfMonth(1);
        LocalDate hasta = cursor.plusMonths(2).withDayOfMonth(1).minusDays(1);
        datos = AgendaDatos.cargar(c, desde, hasta);
        String firma = firmaDe(datos);
        if (!forzar && firma.equals(firmaDatos)) return;     // nada cambió: no reconstruir (evita parpadeos)
        firmaDatos = firma;

        columnas.clear();
        Node contenido = switch (vista) {
            case DIA -> grillaHoraria(List.of(cursor));
            case SEMANA -> grillaHoraria(semana(cursor));
            case MES -> mesView();
            case AGENDA -> agendaView();
        };
        centro.getChildren().setAll(contenido);
        rango.setText(textoRango());
        reconstruirMini();
        actualizarContadores();
    }

    private static String firmaDe(Datos d) {
        StringBuilder sb = new StringBuilder();
        for (Evento e : d.eventos())
            sb.append(e.getId()).append('|').append(e.getTitulo()).append('|').append(e.getFecha()).append('|')
              .append(e.getHoraInicio()).append('|').append(e.getHoraFin()).append('|').append(e.getCategoria()).append(';');
        for (Tarea t : d.tareas())
            sb.append(t.getId()).append('|').append(t.getTitulo()).append('|').append(t.getFecha()).append('|')
              .append(t.getEstado()).append('|').append(t.getProyectoId()).append(';');
        for (Habito h : d.habitos())
            sb.append(h.getId()).append('|').append(h.getNombre()).append('|').append(h.getFrecuencia()).append('|')
              .append(h.isActivo()).append(';');
        for (Contacto k : d.contactos())
            sb.append(k.getId()).append('|').append(k.getNombre()).append('|').append(k.getCumpleanos()).append(';');
        sb.append(d.habitosCumplidos().hashCode());
        return sb.toString();
    }

    private static List<LocalDate> semana(LocalDate ref) {
        LocalDate ini = ref.with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        List<LocalDate> r = new ArrayList<>();
        for (int i = 0; i < 7; i++) r.add(ini.plusDays(i));
        return r;
    }

    private List<LocalDate> rangoVisible() {
        List<LocalDate> r = new ArrayList<>();
        switch (vista) {
            case DIA -> r.add(cursor);
            case SEMANA -> r.addAll(semana(cursor));
            case MES -> { YearMonth ym = YearMonth.from(cursor); for (int i = 1; i <= ym.lengthOfMonth(); i++) r.add(ym.atDay(i)); }
            case AGENDA -> { for (int i = 0; i < 30; i++) r.add(cursor.plusDays(i)); }
        }
        return r;
    }

    private String textoRango() {
        return switch (vista) {
            case DIA -> Ui.formatDate(cursor);
            case MES -> cap(cursor.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + cursor.getYear();
            case AGENDA -> "Próximos 30 días";
            case SEMANA -> {
                List<LocalDate> s = semana(cursor);
                LocalDate a = s.getFirst(), b = s.getLast();
                if (a.getMonth() == b.getMonth()) yield cap(a.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + a.getYear();
                if (a.getYear() == b.getYear()) yield corto(a) + " – " + corto(b) + " de " + a.getYear();
                yield corto(a) + " de " + a.getYear() + " – " + corto(b) + " de " + b.getYear();
            }
        };
    }

    private static String corto(LocalDate d) {
        String m = d.getMonth().getDisplayName(TextStyle.FULL, ES);
        return m.substring(0, Math.min(3, m.length()));
    }

    private void actualizarContadores() {
        List<LocalDate> dias = rangoVisible();
        for (TipoEntrada t : TipoEntrada.values()) {
            int total = 0, hechos = 0;
            for (LocalDate d : dias)
                for (Entrada e : CalendarioUnificadoService.delDia(d, datos, EnumSet.of(t))) {
                    total++;
                    if (e.hecha()) hechos++;
                }
            Label l = contadores.get(t);
            if (l != null) l.setText(total == 0 ? "" : t == TipoEntrada.HABITO ? hechos + "/" + total : String.valueOf(total));
        }
    }

    // =====================================================================================
    //  Vista DÍA / SEMANA: cuadrícula horaria
    // =====================================================================================

    private Node grillaHoraria(List<LocalDate> dias) {
        int n = dias.size();
        LocalDate hoy = LocalDate.now();

        GridPane cab = gridCols(n);
        cab.setPadding(new Insets(0, 14, 0, 0));          // ≈ ancho de la barra de desplazamiento del cuerpo
        GridPane todoDia = gridCols(n);
        todoDia.setPadding(new Insets(0, 14, 6, 0));

        Label zona = new Label(zonaGmt());
        zona.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        zona.setMaxWidth(Double.MAX_VALUE);
        zona.setAlignment(Pos.BOTTOM_CENTER);
        GridPane.setValignment(zona, javafx.geometry.VPos.BOTTOM);
        cab.add(zona, 0, 0);

        GridPane cuerpo = gridCols(n);
        Pane gutter = new Pane();
        gutter.setMinHeight(24 * PX_HORA);
        gutter.setPrefHeight(24 * PX_HORA);
        for (int h = 1; h < 24; h++) {
            Label l = new Label(etiquetaHora(h));
            l.setPrefWidth(GUTTER - 8);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.setLayoutY(h * PX_HORA - 8);
            l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
            gutter.getChildren().add(l);
        }
        cuerpo.add(gutter, 0, 0);

        int limite = n == 1 ? 60 : 3;
        for (int i = 0; i < n; i++) {
            LocalDate d = dias.get(i);
            List<Entrada> es = CalendarioUnificadoService.delDia(d, datos, visibles);

            cab.add(cabeceraDia(d, hoy), i + 1, 0);
            todoDia.add(celdaTodoElDia(d, es.stream().filter(Entrada::todoElDia).toList(), limite, n > 1), i + 1, 0);

            ColumnaDia col = new ColumnaDia(d.equals(hoy));
            for (var cr : CalendarioUnificadoService.asignarCarriles(es.stream().filter(x -> !x.todoElDia()).toList()))
                col.agregar(bloque(cr.entrada()), cr.entrada().minutoInicio(), cr.entrada().minutoFin(), cr.carril(), cr.carriles());
            col.setOnMouseClicked(ev -> {
                int h = (int) Math.max(0, Math.min(23, ev.getY() / PX_HORA));
                EventViews.formEnFechaHora(stage, null, d, String.format("%02d:00", h), null, c, () -> refrescar(true));
            });
            if (col.esHoy) col.setAhora(minutoActual());
            columnas.add(col);
            cuerpo.add(col, i + 1, 0);
        }

        ScrollPane sp = new ScrollPane(cuerpo);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        final double objetivo = vvalue;
        sp.vvalueProperty().addListener((o, a, b) -> vvalue = b.doubleValue());
        alTenerViewport(sp, () -> sp.setVvalue(objetivo >= 0 ? objetivo : vvalueParaHora(sp, 7)));

        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color:" + LINEA + ";");
        VBox caja = new VBox(cab, todoDia, sep, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        caja.setStyle("-fx-background-color:" + PANEL + ";-fx-background-radius:16;");
        return caja;
    }

    private static GridPane gridCols(int n) {
        GridPane g = new GridPane();
        ColumnConstraints g0 = new ColumnConstraints(GUTTER);
        g0.setMinWidth(GUTTER);
        g0.setMaxWidth(GUTTER);
        g.getColumnConstraints().add(g0);
        for (int i = 0; i < n; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(0);
            cc.setPrefWidth(100);
            cc.setFillWidth(true);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    private Node cabeceraDia(LocalDate d, LocalDate hoy) {
        boolean esHoy = d.equals(hoy);
        Label dow = new Label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).toUpperCase(ES).replace(".", ""));
        dow.setStyle("-fx-text-fill:" + (esHoy ? "-accent" : MUTED) + ";-fx-font-size:12;-fx-font-weight:bold;");
        Label num = new Label(String.valueOf(d.getDayOfMonth()));
        num.setMinSize(40, 40);
        num.setPrefSize(40, 40);
        num.setAlignment(Pos.CENTER);
        num.setStyle(esHoy
            ? "-fx-background-color:-accent;-fx-background-radius:20;-fx-text-fill:-on-accent;-fx-font-size:20;-fx-font-weight:bold;"
            : "-fx-text-fill:" + TEXTO + ";-fx-font-size:20;");
        VBox v = new VBox(2, dow, num);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(8, 0, 6, 0));
        v.setCursor(Cursor.HAND);
        v.setOnMouseClicked(e -> irADia(d));
        return v;
    }

    /** Fila "todo el día": tareas, hábitos, cumpleaños y eventos sin hora. */
    private Node celdaTodoElDia(LocalDate d, List<Entrada> lista, int limite, boolean agrupar) {
        VBox v = new VBox(2);
        v.setPadding(new Insets(2, 3, 2, 3));
        v.setMinHeight(26);
        v.setStyle("-fx-border-color:transparent transparent transparent " + LINEA + ";-fx-border-width:0 0 0 1;");
        List<Node> chips = chipsDelDia(d, lista, agrupar);
        int n = 0;
        for (Node ch : chips) {
            if (n++ == limite) {
                Label mas = new Label("+" + (chips.size() - limite) + " más");
                mas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-cursor:hand;-fx-padding:1 6;");
                mas.setOnMouseClicked(e -> irADia(d));
                v.getChildren().add(mas);
                break;
            }
            v.getChildren().add(ch);
        }
        return v;
    }

    /** Un bloque de evento con hora, posicionado por {@link ColumnaDia}. */
    private Node bloque(Entrada e) {
        Label t = new Label(e.titulo());
        t.setWrapText(true);
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:12;");
        Label h = new Label(e.inicio().format(Horas.SALIDA) + " – " + (e.fin() == null ? "" : e.fin().format(Horas.SALIDA)));
        h.setStyle("-fx-text-fill:" + e.tipo().color() + ";-fx-font-size:12;");
        VBox b = new VBox(1, t, h);
        b.setPadding(new Insets(3, 6, 3, 6));
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color:" + e.tipo().fondo() + ";-fx-background-radius:6;"
            + "-fx-border-color:" + e.tipo().color() + ";-fx-border-width:0 0 0 3;-fx-border-radius:6;");
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(b.widthProperty());
        recorte.heightProperty().bind(b.heightProperty());
        b.setClip(recorte);
        Tooltip.install(b, new Tooltip(e.tipo().etiqueta() + ": " + e.titulo()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : "\n" + e.detalle()) + "\nClic para editar"));
        b.setOnMouseClicked(ev -> { ev.consume(); accion(e, ev); });
        return b;
    }

    private static String etiquetaHora(int h) {
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (h < 12 ? " AM" : " PM");
    }

    private static String zonaGmt() {
        int horas = ZonedDateTime.now().getOffset().getTotalSeconds() / 3600;
        return String.format("GMT%+03d", horas);
    }

    private static int minutoActual() {
        LocalTime t = LocalTime.now();
        return t.getHour() * 60 + t.getMinute();
    }

    private void actualizarAhora() {
        int m = minutoActual();
        for (ColumnaDia col : columnas) if (col.esHoy) col.setAhora(m);
    }

    private static double vvalueParaHora(ScrollPane sp, int hora) {
        double max = 24 * PX_HORA - sp.getViewportBounds().getHeight();
        return max <= 0 ? 0 : Math.max(0, Math.min(1, hora * PX_HORA / max));
    }

    /** Ejecuta la acción cuando el ScrollPane ya tiene tamaño (antes no se puede calcular el desplazamiento). */
    private static void alTenerViewport(ScrollPane sp, Runnable r) {
        Bounds b0 = sp.getViewportBounds();
        if (b0 != null && b0.getHeight() > 0) { Platform.runLater(r); return; }
        sp.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override public void changed(ObservableValue<? extends Bounds> o, Bounds a, Bounds b) {
                if (b != null && b.getHeight() > 0) {
                    sp.viewportBoundsProperty().removeListener(this);
                    Platform.runLater(r);
                }
            }
        });
    }

    /** Columna de un día: líneas de hora, bloques posicionados por minuto y línea roja de "ahora". */
    private static final class ColumnaDia extends Pane {
        private record Hueco(Node nodo, int ini, int fin, int carril, int carriles) {}

        final boolean esHoy;
        private final List<Hueco> huecos = new ArrayList<>();
        private final List<Region> lineas = new ArrayList<>();
        private final Region lineaAhora = new Region();
        private final Circle puntoAhora = new Circle(5, Color.web("#E5484D"));
        private int ahora = -1;

        ColumnaDia(boolean esHoy) {
            this.esHoy = esHoy;
            setMinHeight(24 * PX_HORA);
            setPrefHeight(24 * PX_HORA);
            setMaxHeight(24 * PX_HORA);
            setStyle("-fx-border-color:transparent transparent transparent " + LINEA + ";-fx-border-width:0 0 0 1;");
            for (int h = 0; h < 24; h++) {
                Region l = new Region();
                l.setStyle("-fx-background-color:-surface2;");
                l.setMouseTransparent(true);
                lineas.add(l);
                getChildren().add(l);
            }
            lineaAhora.setStyle("-fx-background-color:#E5484D;");
            lineaAhora.setMouseTransparent(true);
            puntoAhora.setMouseTransparent(true);
        }

        void agregar(Node n, int ini, int fin, int carril, int carriles) {
            huecos.add(new Hueco(n, ini, fin, carril, carriles));
            getChildren().add(n);
        }

        void setAhora(int minuto) {
            ahora = minuto;
            if (!getChildren().contains(lineaAhora)) getChildren().addAll(lineaAhora, puntoAhora);
            lineaAhora.toFront();
            puntoAhora.toFront();
            requestLayout();
        }

        @Override protected void layoutChildren() {
            double w = getWidth();
            for (int h = 0; h < lineas.size(); h++) lineas.get(h).resizeRelocate(0, h * PX_HORA, w, 1);
            for (Hueco hu : huecos) {
                double cw = (w - 6) / hu.carriles();
                double x = 2 + hu.carril() * cw;
                double y = hu.ini() * PX_HORA / 60.0;
                double alto = Math.max(20, (hu.fin() - hu.ini()) * PX_HORA / 60.0 - 2);
                hu.nodo().resizeRelocate(x, y, Math.max(10, cw - 2), alto);
            }
            if (ahora >= 0) {
                double y = ahora * PX_HORA / 60.0;
                lineaAhora.resizeRelocate(0, y - 1, w, 2);
                puntoAhora.setCenterX(5);
                puntoAhora.setCenterY(y);
            }
        }
    }

    // =====================================================================================
    //  Vista MES
    // =====================================================================================

    private Node mesView() {
        YearMonth ym = YearMonth.from(cursor);
        LocalDate primero = ym.atDay(1).with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        int off = (int) ChronoUnit.DAYS.between(primero, ym.atDay(1));
        int semanas = (off + ym.lengthOfMonth() + 6) / 7;
        LocalDate hoy = LocalDate.now();

        GridPane g = new GridPane();
        g.setHgap(1);
        g.setVgap(1);
        g.setStyle("-fx-background-color:" + LINEA + ";-fx-background-radius:16;-fx-padding:1;");
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(0);
            g.getColumnConstraints().add(cc);
        }
        g.getRowConstraints().add(new RowConstraints());
        for (int r = 0; r < semanas; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setPrefHeight(100);
            rc.setMinHeight(60);
            g.getRowConstraints().add(rc);
        }
        for (int i = 0; i < 7; i++) {
            LocalDate d = primero.plusDays(i);
            Label l = new Label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).toUpperCase(ES).replace(".", ""));
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            l.setPadding(new Insets(8, 0, 8, 0));
            l.setStyle("-fx-background-color:" + PANEL + ";-fx-text-fill:" + MUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            g.add(l, i, 0);
        }
        for (int i = 0; i < semanas * 7; i++) {
            LocalDate d = primero.plusDays(i);
            boolean delMes = YearMonth.from(d).equals(ym);
            VBox celda = new VBox(2);
            celda.setPadding(new Insets(4));
            celda.setMinSize(0, 0);
            celda.setCursor(Cursor.HAND);
            celda.setStyle("-fx-background-color:" + (delMes ? PANEL : "-bg") + ";");
            Label num = new Label(String.valueOf(d.getDayOfMonth()));
            num.setMinSize(24, 24);
            num.setAlignment(Pos.CENTER);
            num.setStyle(d.equals(hoy)
                ? "-fx-background-color:-accent;-fx-background-radius:12;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
                : "-fx-text-fill:" + (delMes ? TEXTO : "-muted") + ";");
            HBox cabecera = new HBox(num);
            cabecera.setAlignment(Pos.CENTER);
            celda.getChildren().add(cabecera);
            List<Node> chips = chipsDelDia(d, CalendarioUnificadoService.delDia(d, datos, visibles), true);
            int n = 0;
            for (Node ch : chips) {
                if (n++ == 3) {
                    Label mas = new Label("+" + (chips.size() - 3) + " más");
                    mas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-padding:0 6;");
                    celda.getChildren().add(mas);
                    break;
                }
                celda.getChildren().add(ch);
            }
            celda.setOnMouseClicked(e -> irADia(d));
            g.add(celda, i % 7, 1 + i / 7);
        }
        VBox caja = new VBox(g);
        VBox.setVgrow(g, Priority.ALWAYS);
        return caja;
    }

    // =====================================================================================
    //  Vista AGENDA (lista)
    // =====================================================================================

    private Node agendaView() {
        VBox lista = new VBox(6);
        lista.setPadding(new Insets(16));
        LocalDate hoy = LocalDate.now();
        int dias = 0;
        for (int i = 0; i < 30; i++) {
            LocalDate d = cursor.plusDays(i);
            List<Entrada> es = CalendarioUnificadoService.delDia(d, datos, visibles).stream()
                .filter(e -> e.tipo() != TipoEntrada.HABITO || d.equals(hoy)).toList();   // los hábitos diarios solo se listan hoy
            if (es.isEmpty()) continue;
            dias++;
            String rel = d.equals(hoy) ? "Hoy · " : d.equals(hoy.plusDays(1)) ? "Mañana · " : "";
            Label cab = new Label(rel + Ui.formatDate(d));
            cab.setStyle("-fx-text-fill:" + (d.equals(hoy) ? "-accent" : TEXTO) + ";-fx-font-weight:bold;-fx-font-size:14;");
            VBox.setMargin(cab, new Insets(dias == 1 ? 0 : 12, 0, 2, 0));
            lista.getChildren().add(cab);
            for (Entrada e : es) lista.getChildren().add(filaAgenda(e));
        }
        if (dias == 0) {
            Label vacio = new Label("No hay nada en los próximos 30 días con los calendarios seleccionados.");
            vacio.setStyle("-fx-text-fill:" + MUTED + ";");
            lista.getChildren().add(vacio);
        }
        ScrollPane sp = new ScrollPane(lista);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + PANEL + ";-fx-background-color:" + PANEL + ";-fx-background-radius:16;");
        return sp;
    }

    private Node filaAgenda(Entrada e) {
        Label hora = new Label(e.todoElDia() ? "Todo el día"
            : e.inicio().format(Horas.SALIDA) + " – " + (e.fin() == null ? "" : e.fin().format(Horas.SALIDA)));
        hora.setMinWidth(110);
        hora.setStyle("-fx-text-fill:" + MUTED + ";");
        Region punto = new Region();
        punto.setMinSize(10, 10);
        punto.setMaxSize(10, 10);
        punto.setStyle("-fx-background-color:" + e.tipo().color() + ";-fx-background-radius:5;");
        Label t = new Label(e.titulo());
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:14;");
        Label tipoTexto = new Label(e.tipo().etiqueta()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : " · " + e.detalle()));
        tipoTexto.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        HBox tipo = new HBox(5, IconFactory.image(e.tipo().icono(), 14), tipoTexto);
        tipo.setAlignment(Pos.CENTER_LEFT);
        tipo.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        HBox titulo = new HBox(5, IconFactory.image(e.hecha() ? "check" : e.tipo().icono(), 15), t);
        titulo.setAlignment(Pos.CENTER_LEFT);
        VBox textos = new VBox(3, titulo, tipo);
        HBox f = new HBox(12, hora, punto, textos);
        f.setAlignment(Pos.CENTER_LEFT);
        f.setPadding(new Insets(8, 12, 8, 12));
        f.setCursor(Cursor.HAND);
        f.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;");
        if (e.hecha()) f.setOpacity(0.6);
        f.setOnMouseClicked(ev -> accion(e, ev));
        return f;
    }

    // =====================================================================================
    //  Chips y acciones
    // =====================================================================================

    /** Chips de un día; si {@code agrupar}, varios hábitos se resumen en una sola etiqueta. */
    private List<Node> chipsDelDia(LocalDate d, List<Entrada> lista, boolean agrupar) {
        List<Node> r = new ArrayList<>();
        List<Entrada> habitos = lista.stream().filter(e -> e.tipo() == TipoEntrada.HABITO).toList();
        boolean resumir = agrupar && habitos.size() > 1;
        for (Entrada e : lista) {
            if (e.todoElDia() && !(resumir && e.tipo() == TipoEntrada.HABITO)) r.add(chip(e));
            else if (!e.todoElDia() && vista == Vista.MES) r.add(chip(e));      // en el mes también se ven los eventos con hora
        }
        if (resumir) {
            long hechos = habitos.stream().filter(Entrada::hecha).count();
            Label texto = new Label("Hábitos " + hechos + "/" + habitos.size());
            HBox chip = new HBox(4, IconFactory.image("habits", 13), texto);
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setMinWidth(0);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle("-fx-background-color:" + TipoEntrada.HABITO.fondo() + ";-fx-text-fill:" + TipoEntrada.HABITO.color()
                + ";-fx-background-radius:6;-fx-padding:3 6;-fx-font-size:12;-fx-cursor:hand;");
            chip.setOnMouseClicked(ev -> { ev.consume(); irADia(d); });
            Tooltip.install(chip, new Tooltip("Clic para ver y marcar tus hábitos de este día"));
            r.add(chip);
        }
        return r;
    }

    private Node chip(Entrada e) {
        String hora = e.todoElDia() ? "" : e.inicio().format(Horas.SALIDA) + " ";
        Label text = new Label(hora + e.titulo());
        text.setMaxWidth(Double.MAX_VALUE);
        text.setMinWidth(0);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setStyle("-fx-text-fill:" + e.tipo().color() + ";-fx-font-size:12;");
        HBox chip = new HBox(4, IconFactory.image(e.hecha() ? "check" : e.tipo().icono(), 13), text);
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color:" + e.tipo().fondo()
            + ";-fx-background-radius:6;-fx-padding:3 6;-fx-cursor:hand;");
        if (e.hecha()) chip.setOpacity(0.65);
        Tooltip.install(chip, new Tooltip(e.tipo().etiqueta() + ": " + e.titulo()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : "\n" + e.detalle()) + "\n" + ayuda(e)));
        chip.setOnMouseClicked(ev -> { ev.consume(); accion(e, ev); });
        return chip;
    }

    private static String ayuda(Entrada e) {
        return switch (e.origen()) {
            case Tarea t -> "Clic: marcar/desmarcar · Clic derecho: editar";
            case Habito h -> "Clic: marcar/desmarcar este día";
            case Contacto k -> "Clic: ver datos del contacto";
            default -> "Clic: editar";
        };
    }

    private void accion(Entrada e, MouseEvent ev) {
        boolean secundario = ev.getButton() == MouseButton.SECONDARY;
        Object o = e.origen();
        if (o instanceof Evento evento) {
            EventViews.form(stage, evento, c, () -> refrescar(true));
        } else if (o instanceof Tarea t) {
            if (secundario) TaskViews.form(t, c, () -> refrescar(true));
            else c.alternarCompletada(t).ifPresent(r -> Toast.mostrar(view, c.mensajeProgreso(r)));
        } else if (o instanceof Habito h) {
            marcarHabito(h, e.fecha());
        } else if (o instanceof Contacto k) {
            StringBuilder sb = new StringBuilder(e.detalle().isBlank() ? "" : e.detalle() + "\n");
            if (k.getTelefono() != null && !k.getTelefono().isBlank()) sb.append("Teléfono: ").append(k.getTelefono()).append('\n');
            if (k.getEmail() != null && !k.getEmail().isBlank()) sb.append("Email: ").append(k.getEmail()).append('\n');
            sb.append("\nPuedes editar este contacto en CRM Personal.");
            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            a.initOwner(stage);
            a.setTitle("Cumpleaños");
            a.setHeaderText(k.getNombre());
            a.showAndWait();
        }
    }

    private void marcarHabito(Habito h, LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            Toast.mostrar(view, "Aún no puedes marcar un hábito de un día futuro");
            return;
        }
        boolean yaHecho = datos.habitosCumplidos().getOrDefault(fecha.toString(), Set.of()).contains(h.getId());
        if (yaHecho) habitoDAO.desmarcar(h.getId(), fecha.toString());
        else habitoDAO.marcar(h.getId(), fecha.toString());
        refrescar(true);
        if (!yaHecho) Toast.mostrar(view, h.getNombre() + " · racha " + habitoDAO.racha(h.getId()) + " días");
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }
}

