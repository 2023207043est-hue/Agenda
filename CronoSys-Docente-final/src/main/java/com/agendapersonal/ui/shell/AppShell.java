package com.agendapersonal.ui.shell;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.infrastructure.db.ConfigDAO;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.DashboardView;
import com.agendapersonal.ui.EventViews;
import com.agendapersonal.ui.FontPreferences;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Logo;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.ui.RelojPrefs;
import com.agendapersonal.ui.Switch;
import com.agendapersonal.ui.TaskViews;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.ui.modules.AgendaUnificadaView;
import com.agendapersonal.ui.modules.CalendarioModule;
import com.agendapersonal.ui.modules.CrmView;
import com.agendapersonal.ui.modules.EspacioEstudioView;
import com.agendapersonal.ui.modules.FinanzasView;
import com.agendapersonal.ui.modules.HabitosView;
import com.agendapersonal.ui.modules.NotasView;
import com.agendapersonal.ui.modules.SaludView;
import com.agendapersonal.ui.modules.TareasProyectosView;
import com.agendapersonal.ui.modules.TimeBlockingView;
import com.agendapersonal.ui.modules.EspacioDocenteView;
import com.agendapersonal.ui.notify.CentroAvisos;
import com.agendapersonal.service.MetodoEnfoque;
import com.agendapersonal.util.Ui;
import com.agendapersonal.util.Perfil;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal: menú lateral que se puede contraer a solo íconos, barra superior con fecha,
 * cambio rápido de tema y campana de avisos, y una página por módulo (se guardan en caché).
 */
public class AppShell {
    private static final double SIDEBAR_OPEN = 252, SIDEBAR_CLOSED = 76;
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");

    /** Páginas del menú: clave interna → texto visible, ícono y sección. */
    private record Item(String key, String label, String icon, String section) {}
    private static final List<Item> ITEMS = List.of(
        new Item("Inicio", "Inicio", "home", "Principal"),
        new Item("Agenda Unificada", "Calendario", "agenda", "Principal"),
        new Item("Time Blocking", "Time blocking", "clock", "Principal"),
        new Item("Espacio de Estudio", "Estudio", "book", "Principal"),
        new Item("Tareas y Proyectos", "Tareas", "tasks", "Organizar"),
        new Item("Notas y PKM", "Notas", "notes", "Organizar"),
        new Item("CRM Personal", "Contactos", "contacts", "Personal"),
        new Item("Hábitos", "Hábitos", "habits", "Personal"),
        new Item("Finanzas", "Finanzas", "finance", "Personal"),
        new Item("Salud", "Salud", "health", "Personal"));
    private static final Item CONFIG = new Item("Configuración", "Configuración", "settings", "Ajustes");

    private List<Item> itemsVisibles() {
        if (!Perfil.esDocente()) return ITEMS;
        return ITEMS.stream().map(i -> i.key().equals("Espacio de Estudio")
            ? new Item(i.key(), "Espacio Docente", "book", i.section()) : i).toList();
    }

    /** Cada sección de ajustes corresponde a una página: al cambiarla, la página se rehace. */
    private static final Map<String, String> PAGINA_POR_SECCION = Map.ofEntries(
        Map.entry("home", "Inicio"), Map.entry("calendar", "Agenda Unificada"),
        Map.entry("timeBlocking", "Time Blocking"), Map.entry("study", "Espacio de Estudio"),
        Map.entry("tasks", "Tareas y Proyectos"), Map.entry("notes", "Notas y PKM"),
        Map.entry("contacts", "CRM Personal"), Map.entry("habits", "Hábitos"),
        Map.entry("finance", "Finanzas"), Map.entry("health", "Salud"));

    private final MainController c;
    private final Stage stage;
    private final ThemeService theme = ThemeService.get();
    private final ConfigDAO cfg = new ConfigDAO();
    private final StackPane root = new StackPane();
    private final BorderPane frame = new BorderPane();
    private final StackPane content = new StackPane();
    private final VBox toastLayer = new VBox();
    private final Map<String, Node> cache = new HashMap<>();
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final List<Node> soloExpandido = new ArrayList<>();
    private final List<Node> soloContraido = new ArrayList<>();
    private TimeBlockingView timeBlocking;
    private VBox sidebar;
    private Button toggleSidebar;
    private Label breadcrumb;
    private Label dateLabel;
    private boolean collapsed;
    private String actual;

    public AppShell(MainController c, Stage stage) {
        this.c = c; this.stage = stage;
        root.getStyleClass().add("app-root");
        frame.getStyleClass().add("app-frame");
        content.getStyleClass().add("content");
        collapsed = "1".equals(cfg.get("ui.sidebar.collapsed", "0"));
        frame.setLeft(nav());
        VBox center = new VBox(topbar(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        frame.setCenter(center);
        Notifier.install(toastLayer);
        Notifier.setEnabled("1".equals(cfg.get("ui.confirmaciones", "0")));
        root.getChildren().addAll(frame, toastLayer);
        aplicarContraido(false);
        // Al cambiar de perfil, importar o restablecer, las ventanas se vuelven a construir con los ajustes nuevos.
        com.agendapersonal.settings.SettingsStore.get().addListener(sec -> {
            if (sec.equals("*")) javafx.application.Platform.runLater(() -> { cache.clear(); go(actual); });
        });
        go("Inicio");
    }

    public StackPane getRoot() { return root; }

    /** Atajos: Ctrl+B contrae el menú, Ctrl+Shift+L cambia claro/oscuro. */
    public void instalarAtajos(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::toggleSidebar);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), this::pedirSalir);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            this::cambiarTema);
    }

    /** Navega a un módulo por su clave (lo usan las notificaciones: "Abrir agenda"). */
    public void mostrar(String titulo) { go(titulo); }

    private Node page(String key, Supplier<Node> build) {
        return cache.computeIfAbsent(key, k -> build.get());
    }

    private Node panelPage() {
        return page("Inicio", () -> new DashboardView(c, stage,
            () -> EventViews.form(stage, null, c, () -> {}),
            q -> EventViews.manage(q == null ? "" : q, c, stage, () -> {}),
            () -> TaskViews.form(null, c, () -> {}),
            () -> prepararClase()).getView());
    }

    /** Abre el Time Blocking con la técnica de preparación de clase (solo para docentes). */
    private Runnable prepararClase() {
        return () -> {
            page("Time Blocking", this::crearTimeBlocking);
            if (timeBlocking != null) timeBlocking.seleccionar(MetodoEnfoque.DOCENTES.getFirst());
            go("Time Blocking");
        };
    }

    /** Crea el Time Blocking conservando la instancia de la vista para poder seleccionar métodos desde fuera. */
    private Node crearTimeBlocking() {
        timeBlocking = new TimeBlockingView(c);
        return timeBlocking.getView();
    }

    private void go(String key) {
        if (key.equals("Panel")) key = "Inicio";
        if (key.equals("Calendario")) key = "Agenda Unificada";   // el calendario y la agenda ahora son una sola ventana
        Node n = switch (key) {
            case "Configuración" -> page(key, this::settingsPage);
            case "Agenda Unificada" -> page(key, () -> new AgendaUnificadaView(c, stage).getView());
            case "Time Blocking" -> page(key, this::crearTimeBlocking);
            case "Espacio de Estudio" -> page(key, () -> Perfil.esDocente()
                ? new EspacioDocenteView(c, stage, () -> go("Time Blocking"), () -> go("Agenda Unificada")).getView()
                : new EspacioEstudioView(c).getView());
            case "Tareas y Proyectos" -> page(key, () -> new TareasProyectosView(c).getView());
            case "Notas y PKM" -> page(key, () -> new NotasView().getView());
            case "CRM Personal" -> page(key, () -> new CrmView().getView());
            case "Hábitos" -> page(key, () -> new HabitosView().getView());
            case "Finanzas" -> page(key, () -> new FinanzasView().getView());
            case "Salud" -> page(key, () -> new SaludView().getView());
            default -> { key = "Inicio"; yield panelPage(); }
        };
        boolean cambia = !key.equals(actual);
        actual = key;
        content.getChildren().setAll(n);
        if (cambia) Anim.pageIn(n);
        botones.forEach((k, b) -> b.pseudoClassStateChanged(CURRENT, k.equals(actual)));
        Item it = itemsVisibles().stream().filter(i -> i.key().equals(actual)).findFirst().orElse(CONFIG);
        breadcrumb.setText(it.section() + "  ›  " + it.label());
    }

    // ------------------------------------------------------------------ menú lateral

    private VBox nav() {
        sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 14, 16, 14));
        sidebar.setMinWidth(Region.USE_PREF_SIZE);
        sidebar.setMaxWidth(Region.USE_PREF_SIZE);
        sidebar.setPrefWidth(collapsed ? SIDEBAR_CLOSED : SIDEBAR_OPEN);

        HBox brand = Logo.wordmark(36);
        Node marca = brand.getChildren().getFirst();
        Node nombre = brand.getChildren().get(1);
        soloExpandido.add(nombre);
        brand.setPadding(new Insets(0, 6, 18, 6));
        brand.setOnMouseClicked(e -> go("Inicio"));
        brand.setCursor(javafx.scene.Cursor.HAND);
        marca.setAccessibleText("Compás - ir a inicio");
        sidebar.getChildren().add(brand);

        VBox lista = new VBox(2);
        String seccion = null;
        List<Item> visibles = itemsVisibles();
        for (Item it : visibles) {
            if (!it.section().equals(seccion)) {
                seccion = it.section();
                Label l = new Label(seccion.toUpperCase(Ui.ES));
                l.getStyleClass().add("side-section");
                VBox.setMargin(l, new Insets(14, 10, 6, 12));
                Region linea = new Region();
                linea.getStyleClass().add("side-divider");
                VBox.setMargin(linea, new Insets(12, 14, 8, 14));
                soloExpandido.add(l);
                soloContraido.add(linea);
                if (it != visibles.getFirst()) lista.getChildren().addAll(l, linea);
                else lista.getChildren().addAll(l);
            }
            lista.getChildren().add(navBtn(it));
        }
        ScrollPane sc = new ScrollPane(lista);
        sc.setFitToWidth(true);
        sc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sc.getStyleClass().add("side-scroll");
        VBox.setVgrow(sc, Priority.ALWAYS);

        toggleSidebar = new Button("Contraer menú");
        toggleSidebar.getStyleClass().addAll("nav-btn", "nav-toggle");
        toggleSidebar.setMaxWidth(Double.MAX_VALUE);
        toggleSidebar.setGraphicTextGap(12);
        toggleSidebar.setMnemonicParsing(false);
        toggleSidebar.setOnAction(e -> toggleSidebar());
        Button config = navBtn(CONFIG);
        sidebar.getChildren().addAll(sc, toggleSidebar, config);
        return sidebar;
    }

    private Button navBtn(Item it) {
        Button btn = new Button(it.label(), IconFactory.icon(it.icon(), 19));
        btn.getStyleClass().add("nav-btn");
        btn.setGraphicTextGap(12);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMnemonicParsing(false);
        btn.setAccessibleText(it.label());
        btn.setOnAction(e -> go(it.key()));
        botones.put(it.key(), btn);
        return btn;
    }

    /** Contrae o expande el menú lateral (también con Ctrl+B). */
    public void alternarMenu() { toggleSidebar(); }

    private void toggleSidebar() {
        collapsed = !collapsed;
        cfg.set("ui.sidebar.collapsed", collapsed ? "1" : "0");
        aplicarContraido(true);
    }

    private void aplicarContraido(boolean animar) {
        double target = collapsed ? SIDEBAR_CLOSED : SIDEBAR_OPEN;
        sidebar.pseudoClassStateChanged(PseudoClass.getPseudoClass("collapsed"), collapsed);
        toggleSidebar.setText(collapsed ? "Expandir menú" : "Contraer menú");
        double iconSize = collapsed ? 22 : 19;
        toggleSidebar.setGraphic(IconFactory.icon(collapsed ? "sidebar-expand" : "sidebar-collapse", iconSize));
        botones.forEach((key, button) -> {
            Item item = itemsVisibles().stream().filter(candidate -> candidate.key().equals(key)).findFirst().orElse(CONFIG);
            button.setGraphic(IconFactory.icon(item.icon(), iconSize));
        });
        Runnable textos = () -> {
            for (Button b : todosLosBotones()) {
                b.setContentDisplay(collapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
                b.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                if (collapsed) Ui.tooltip(b, b.getText()); else b.setTooltip(null);
            }
            soloExpandido.forEach(n -> { n.setVisible(!collapsed); n.setManaged(!collapsed); });
            soloContraido.forEach(n -> { n.setVisible(collapsed); n.setManaged(collapsed); });
        };
        if (!animar) { textos.run(); sidebar.setPrefWidth(target); return; }
        if (collapsed) textos.run();          // al contraer, primero se ocultan los textos
        Timeline t = new Timeline(new KeyFrame(Duration.millis(240),
            new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.SPLINE(0.2, 0.9, 0.2, 1))));
        if (!collapsed) t.setOnFinished(e -> { textos.run(); soloExpandido.forEach(n -> Anim.fadeIn(n, 160)); });
        t.play();
    }

    private void seleccionarRol(String rol) {
        Perfil.guardarRol(rol);
        cache.clear(); botones.clear(); soloExpandido.clear(); soloContraido.clear();
        actual = null;
        frame.setLeft(nav());
        aplicarContraido(false);
        go("Inicio");
    }

    private List<Button> todosLosBotones() {
        List<Button> l = new ArrayList<>(botones.values());
        l.add(toggleSidebar);
        return l;
    }

    // ------------------------------------------------------------------ barra superior

    private HBox topbar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        breadcrumb = new Label();
        breadcrumb.getStyleClass().add("breadcrumb");

        dateLabel = new Label();
        dateLabel.setGraphic(IconFactory.icon("calendar", 16));
        dateLabel.setGraphicTextGap(8);
        dateLabel.getStyleClass().add("date-chip");
        Runnable fecha = () -> dateLabel.setText(Ui.formatDateShort(LocalDate.now()));
        fecha.run();
        Timeline reloj = new Timeline(new KeyFrame(Duration.minutes(1), e -> fecha.run()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();

        Button temaBtn = Ui.iconButton(theme.isDark() ? "sun" : "moon",
            theme.isDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
        temaBtn.getStyleClass().add("topbar-button");
        temaBtn.setOnAction(e -> cambiarTema());
        theme.revisionProperty().addListener((o, a, b) -> {
            Node ic = IconFactory.icon(theme.isDark() ? "sun" : "moon", 18);
            temaBtn.setGraphic(ic);
            Ui.tooltip(temaBtn, theme.isDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
            RotateTransition r = new RotateTransition(Duration.millis(380), ic);
            r.setFromAngle(-90);
            r.setToAngle(0);
            r.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
            r.play();
        });

        Button salirBtn = Ui.iconButton("power", "Salir de Compás (Ctrl+Q)");
        salirBtn.getStyleClass().add("topbar-button");
        salirBtn.setOnAction(e -> pedirSalir());
        bar.getChildren().addAll(breadcrumb, Ui.spacer(), dateLabel, temaBtn, campana(), salirBtn);
        return bar;
    }

    private Runnable alSalir = () -> {};
    public void setAlSalir(Runnable r) { alSalir = r == null ? () -> {} : r; }

    /** Pide confirmación (una sola vez por clic) y sale con la despedida. */
    private void pedirSalir() {
        if (Ui.confirm(root, "¿Salir de Compás?", "Dejarás de recibir avisos de eventos hasta que vuelvas a abrirlo. Para seguir con avisos, cierra la ventana con la X y quedará en la bandeja.", "Salir")) alSalir.run();
    }

    private void cambiarTema() {
        theme.toggleMode();   // sin aviso: el cambio se ve a simple vista
    }

    /** Campana con contador de avisos no leídos y su historial. */
    private StackPane campana() {
        Button bell = Ui.iconButton("notification", "Avisos recientes");
        bell.getStyleClass().add("topbar-button");
        Label badge = new Label();
        badge.getStyleClass().add("bell-badge");
        badge.setMouseTransparent(true);
        badge.visibleProperty().bind(Notifier.unreadProperty().greaterThan(0));
        badge.textProperty().bind(Notifier.unreadProperty().asString());
        StackPane s = new StackPane(bell, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-2, -3, 0, 0));

        Popup pop = new Popup();
        pop.setAutoHide(true);
        ListView<Notifier.Entry> lista = new ListView<>(Notifier.history());
        lista.getStyleClass().add("history-list");
        lista.setPrefSize(360, 380);
        lista.setPlaceholder(Ui.empty("notification", "Sin avisos", "Aquí verás los cambios y recordatorios recientes."));
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
        lista.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Notifier.Entry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); setText(null); return; }
                StackPane ic = new StackPane(IconFactory.icon(e.tone().icon(), 16));
                ic.getStyleClass().addAll("toast-badge", e.tone().css());
                Label t = new Label(e.title());
                t.getStyleClass().add("history-title");
                t.setWrapText(true);
                VBox tx = new VBox(2, t);
                if (e.message() != null && !e.message().isBlank()) {
                    Label m = new Label(e.message());
                    m.getStyleClass().add("history-text");
                    m.setWrapText(true);
                    tx.getChildren().add(m);
                }
                tx.setMaxWidth(250);
                Label h = new Label(e.when().format(hora));
                h.getStyleClass().add("caption");
                HBox row = new HBox(10, ic, tx, Ui.spacer(), h);
                row.setAlignment(Pos.TOP_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        Label titulo = Ui.h3("Avisos recientes");
        Button limpiar = Ui.ghost(null, "Limpiar");
        limpiar.setOnAction(e -> Notifier.history().clear());
        HBox head = new HBox(8, titulo, Ui.spacer(), limpiar);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(10, head, lista);
        panel.getStyleClass().add("popover");
        panel.setPadding(new Insets(14));
        pop.getContent().add(panel);
        bell.setOnAction(e -> {
            if (pop.isShowing()) { pop.hide(); return; }
            var p = bell.localToScreen(bell.getBoundsInLocal());
            pop.show(stage, p.getMaxX() - 390, p.getMaxY() + 8);
            Anim.fadeIn(panel, 160);
            Notifier.markAllRead();
        });
        return s;
    }

    // ------------------------------------------------------------------ configuración

    private Node settingsPage() {
        VBox page = Ui.page();
        page.getChildren().add(Ui.pageHeader("settings", "Configuración",
            "Ajusta la apariencia y los avisos de Compás a tu gusto."));

        // Perfil
        javafx.scene.control.TextField nombreCampo = new javafx.scene.control.TextField(com.agendapersonal.util.Perfil.nombre());
        nombreCampo.setPromptText("Tu nombre");
        Button guardarNombre = Ui.secondary(null, "Guardar");
        guardarNombre.setOnAction(e -> {
            com.agendapersonal.util.Perfil.guardarNombre(nombreCampo.getText());
            nombreCampo.setText(com.agendapersonal.util.Perfil.nombre());
            Notifier.success("Nombre guardado", null);
        });
        HBox nombreFila = new HBox(10, nombreCampo, guardarNombre);
        HBox.setHgrow(nombreCampo, Priority.ALWAYS);
        javafx.scene.control.ComboBox<String> rolUsuario = new javafx.scene.control.ComboBox<>();
        rolUsuario.getItems().setAll("Estudiante", "Docente");
        rolUsuario.setValue(Perfil.esDocente() ? "Docente" : "Estudiante");
        Button guardarRol = Ui.secondary(null, "Cambiar espacio");
        guardarRol.setOnAction(e -> {
            String rol = "Docente".equals(rolUsuario.getValue()) ? Perfil.DOCENTE : Perfil.ESTUDIANTE;
            if (!rol.equals(Perfil.rol())) seleccionarRol(rol);
        });
        HBox rolFila = new HBox(10, rolUsuario, guardarRol);
        rolFila.setAlignment(Pos.CENTER_LEFT);
        VBox perfil = Ui.card();
        perfil.getChildren().addAll(Ui.h2("Tu perfil"),
            Ui.muted("Compás es solo para ti y se guarda en este equipo. El perfil cambia las herramientas del espacio de trabajo."),
            Ui.field("Nombre", nombreFila), Ui.field("Usar Compás como", rolFila));

        Button borrarDatos = Ui.secondary("trash", "Borrar todos mis datos");
        borrarDatos.setOnAction(e -> confirmarBorradoTotal());
        VBox datos = Ui.card();
        datos.getChildren().addAll(Ui.h2("Restablecer Compás"),
            Ui.muted("Borra tareas, eventos, bloques, notas, proyectos, contactos, hábitos, finanzas, salud, perfiles y preferencias. No se puede deshacer; después podrás configurar Compás de nuevo."),
            borrarDatos);

        // Reloj del inicio
        Switch relojVisible = new Switch("Mostrar el reloj grande", "Aparece en la pantalla de inicio.", RelojPrefs.visible());
        relojVisible.selectedProperty().addListener((o, a, v) -> RelojPrefs.setVisible(v));
        Switch reloj24 = new Switch("Formato de 24 horas", "Si lo apagas se muestra con AM/PM.", RelojPrefs.formato24());
        reloj24.selectedProperty().addListener((o, a, v) -> RelojPrefs.setFormato24(v));
        Switch relojSeg = new Switch("Mostrar segundos", null, RelojPrefs.segundos());
        relojSeg.selectedProperty().addListener((o, a, v) -> RelojPrefs.setSegundos(v));
        Ui.Segmented relojTam = new Ui.Segmented("Pequeño", "Mediano", "Grande");
        relojTam.select(RelojPrefs.tamano().ordinal());
        relojTam.selectedProperty().addListener((o, a, v) -> RelojPrefs.setTamano(RelojPrefs.Tamano.values()[v.intValue()]));
        VBox relojCard = Ui.card();
        relojCard.getChildren().addAll(Ui.h2("Reloj"), relojVisible, sep(), reloj24, sep(), relojSeg, sep(),
            Ui.field("Tamaño", relojTam.node()));

        // Apariencia
        Ui.Segmented modo = new Ui.Segmented("Claro", "Oscuro");
        modo.button(0).setGraphic(IconFactory.icon("sun", 16));
        modo.button(1).setGraphic(IconFactory.icon("moon", 16));
        modo.select(theme.isDark() ? 1 : 0);
        modo.selectedProperty().addListener((o, a, v) -> {
            theme.setMode(v.intValue() == 1 ? ThemeService.Mode.OSCURO : ThemeService.Mode.CLARO);
        });
        theme.revisionProperty().addListener((o, a, b) -> modo.select(theme.isDark() ? 1 : 0));

        FlowPane swatches = new FlowPane(10, 10);
        List<StackPane> sw = new ArrayList<>();
        for (ThemeService.Accent ac : ThemeService.Accent.values()) {
            Circle dot = new Circle(13);
            dot.setStyle("-fx-fill:" + ac.color(theme.isDark()) + ";");
            Node check = IconFactory.icon("check", 16, "tint-on-accent");
            check.setVisible(ac == theme.accent());
            StackPane s = new StackPane(dot, check);
            s.getStyleClass().add("swatch");
            s.setAccessibleText(ac.toString());
            Label name = new Label(ac.toString());
            name.getStyleClass().add("caption");
            VBox col = new VBox(6, s, name);
            col.setAlignment(Pos.CENTER);
            col.setOnMouseClicked(e -> {
                theme.setAccent(ac);
                Anim.pop(s);
            });
            col.setCursor(javafx.scene.Cursor.HAND);
            s.getProperties().put("accent", ac);
            sw.add(s);
            swatches.getChildren().add(col);
        }
        theme.revisionProperty().addListener((o, a, b) -> sw.forEach(s -> {
            ThemeService.Accent ac = (ThemeService.Accent) s.getProperties().get("accent");
            s.getChildren().get(0).setStyle("-fx-fill:" + ac.color(theme.isDark()) + ";");
            s.getChildren().get(1).setVisible(ac == theme.accent());
        }));

        VBox apariencia = Ui.card();
        apariencia.getChildren().addAll(Ui.h2("Apariencia"),
            Ui.muted("Los cambios se aplican al instante en todas las ventanas y se guardan solos."),
            Ui.field("Tema", modo.node()), Ui.field("Color de acento", swatches));

        // Tipografía
        javafx.scene.control.ComboBox<String> fuente = new javafx.scene.control.ComboBox<>();
        fuente.getItems().setAll(FontPreferences.options());
        fuente.setValue(FontPreferences.family());
        fuente.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty ? null : f + (f != null && FontPreferences.esIncluida(f) ? "  · incluida" : ""));
                setStyle(empty || f == null ? "" : "-fx-font-family:\"" + f + "\";");
            }
        });
        fuente.setOnAction(e -> {
            if (fuente.getValue() == null || fuente.getValue().equals(FontPreferences.family())) return;
            FontPreferences.setFamily(fuente.getValue());
        });
        Ui.Segmented tam = new Ui.Segmented("Pequeño", "Normal", "Grande", "Muy grande");
        tam.select(FontPreferences.size().ordinal());
        tam.selectedProperty().addListener((o, a, v) -> {
            FontPreferences.setSize(FontPreferences.Size.values()[v.intValue()]);
        });
        Label muestra = new Label("Así se verán tus textos: Reunión con el equipo a las 10:30 · S/ 1.250,00");
        muestra.getStyleClass().add("font-preview");
        muestra.setWrapText(true);
        VBox tipografia = Ui.card();
        tipografia.getChildren().addAll(Ui.h2("Tipografía"),
            Ui.muted("Las fuentes marcadas como «incluida» vienen con Compás y se ven igual en cualquier PC."),
            Ui.field("Fuente", fuente), Ui.field("Tamaño del texto", tam.node()), muestra);

        // Avisos
        Switch cambios = new Switch("Confirmaciones al guardar", "Muestra un mensaje breve al guardar, eliminar o completar algo. Los recordatorios de eventos no dependen de esto.",
            Notifier.isEnabled());
        cambios.selectedProperty().addListener((o, a, v) -> {
            Notifier.setEnabled(v);
            cfg.set("ui.confirmaciones", v ? "1" : "0");
        });
        Switch eventos = new Switch("Avisar cuando empieza un evento",
            "Aparece una notificación en el escritorio justo a la hora de inicio, aunque Compás esté minimizado.",
            !"0".equals(cfg.get(CentroAvisos.CFG_EVENTOS, "1")));
        eventos.selectedProperty().addListener((o, a, v) -> cfg.set(CentroAvisos.CFG_EVENTOS, v ? "1" : "0"));
        Switch cumples = new Switch("Avisar cumpleaños del día", "Una sola notificación el mismo día del cumpleaños.",
            !"0".equals(cfg.get(CentroAvisos.CFG_CUMPLES, "1")));
        cumples.selectedProperty().addListener((o, a, v) -> cfg.set(CentroAvisos.CFG_CUMPLES, v ? "1" : "0"));
        Switch resumen = new Switch("Resumen al abrir Compás",
            "Ventana con los pendientes del día al iniciar. También está en el ícono junto al reloj.",
            "1".equals(cfg.get(CentroAvisos.CFG_RESUMEN, "0")));
        resumen.selectedProperty().addListener((o, a, v) -> cfg.set(CentroAvisos.CFG_RESUMEN, v ? "1" : "0"));
        VBox avisos = Ui.card();
        avisos.getChildren().addAll(Ui.h2("Avisos"),
            Ui.muted("Los avisos de escritorio solo salen para lo que está pasando en el momento."),
            cambios, sep(), eventos, sep(), cumples, sep(), resumen);

        // Acerca de
        HBox about = new HBox(16, Logo.mark(56), new VBox(4, Ui.h2("Compás 1.1"),
            Ui.muted("Tu tiempo, mejor invertido. Atajos: Ctrl+B contrae el menú · Ctrl+Shift+L cambia el tema.")));
        about.setAlignment(Pos.CENTER_LEFT);
        VBox acerca = Ui.card();
        acerca.getChildren().add(about);

        HBox fila = new HBox(20, apariencia, tipografia);
        HBox.setHgrow(apariencia, Priority.ALWAYS);
        HBox.setHgrow(tipografia, Priority.ALWAYS);
        apariencia.setMaxWidth(Double.MAX_VALUE);
        tipografia.setMaxWidth(Double.MAX_VALUE);
        apariencia.setPrefWidth(400);
        tipografia.setPrefWidth(400);
        HBox fila2 = new HBox(20, perfil, relojCard);
        HBox.setHgrow(perfil, Priority.ALWAYS);
        HBox.setHgrow(relojCard, Priority.ALWAYS);
        perfil.setPrefWidth(400);
        relojCard.setPrefWidth(400);
        page.getChildren().addAll(fila2, fila,
            datos,
            com.agendapersonal.ui.settings.PersonalizacionCards.temas(),
            com.agendapersonal.ui.settings.PersonalizacionCards.crearTema(),
            com.agendapersonal.ui.settings.PersonalizacionCards.forma(),
            com.agendapersonal.ui.settings.PersonalizacionCards.perfiles(),
            com.agendapersonal.ui.settings.PersonalizacionCards.respaldo(),
            avisos, com.agendapersonal.ui.settings.PersonalizacionCards.silencio(), acerca);
        return Ui.scroll(page);
    }

    private void confirmarBorradoTotal() {
        javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(stage);
        confirmacion.setTitle("Borrar todos los datos");
        confirmacion.setHeaderText("¿Restablecer Compás por completo?");
        confirmacion.setContentText("Se eliminarán permanentemente tus registros y preferencias. A continuación podrás volver a elegir tu nombre y perfil.");
        javafx.scene.control.ButtonType borrar = new javafx.scene.control.ButtonType(
            "Borrar todo", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        confirmacion.getButtonTypes().setAll(borrar, javafx.scene.control.ButtonType.CANCEL);
        confirmacion.setOnShown(e -> theme.applyTo(confirmacion.getDialogPane().getScene()));
        if (confirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != borrar) return;

        try {
            com.agendapersonal.infrastructure.db.Database.borrarTodosLosDatos();
            theme.settings().resetAll();
            c.cargarTodo();
            com.agendapersonal.ui.BienvenidaView.mostrarSiHaceFalta();
            seleccionarRol(Perfil.rol());

            javafx.scene.control.Alert listo = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Compás quedó restablecido. Tus datos anteriores ya se eliminaron.");
            listo.initOwner(stage);
            listo.setTitle("Datos borrados");
            listo.setHeaderText("Restablecimiento completado");
            listo.setOnShown(e -> theme.applyTo(listo.getDialogPane().getScene()));
            listo.showAndWait();
        } catch (RuntimeException ex) {
            javafx.scene.control.Alert error = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "No se pudieron borrar los datos. " + ex.getMessage());
            error.initOwner(stage);
            error.setTitle("No se pudo restablecer Compás");
            error.showAndWait();
        }
    }

    private static Region sep() {
        Region r = new Region();
        r.getStyleClass().add("hairline");
        return r;
    }
}
