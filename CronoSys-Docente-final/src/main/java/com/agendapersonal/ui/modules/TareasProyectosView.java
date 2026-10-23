package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.Proyecto;
import com.agendapersonal.infrastructure.repo.ProyectoDAO;
import com.agendapersonal.model.EstadoTarea;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.ProgresoService;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.ui.TaskViews;
import com.agendapersonal.util.Ui;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tareas y proyectos: proyectos con su avance a la izquierda y tareas agrupadas por fecha a la derecha. */
public final class TareasProyectosView {
    private static final String[] COLORES = {"#2F5BB7", "#C64B1C", "#2C7A4B", "#6340B8", "#1F6F6A", "#B23A55", "#8A5A00"};
    /** Elemento "Todas las tareas" de la lista de proyectos. */
    private static final Proyecto TODAS = new Proyecto(-1, "Todas las tareas", "", "Activo", null, null, null);

    private final MainController c;
    private final ProyectoDAO dao = new ProyectoDAO();
    private final VBox view;
    private final ListView<Proyecto> proyectos = new ListView<>();
    private final VBox grupos = new VBox(0);
    private final TextField buscar = new TextField();
    private final Ui.Segmented filtro = new Ui.Segmented("Todas", "Hoy", "Próximas", "Vencidas", "Completadas");
    private final Ui.Segmented estadoProyectos = new Ui.Segmented("Activos", "Todos");
    private boolean verCompletadas;

    public TareasProyectosView(MainController c) {
        this.c = c;
        VBox page = Ui.page();
        Button nueva = Ui.primary("add", "Nueva tarea");
        nueva.setOnAction(e -> TaskViews.form(null, c, this::fillTareas));
        page.getChildren().add(Ui.pageHeader("tasks", "Tareas",
            "Todo lo que tienes pendiente, ordenado por lista y por fecha.", nueva));

        HBox body = new HBox(24, panelProyectos(), panelTareas());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().add(body);
        view = page;

        c.getTareas().addListener((Observable o) -> { fillTareas(); proyectos.refresh(); });
        fillProyectos();
    }

    // ------------------------------------------------------------------ proyectos

    private VBox panelProyectos() {
        Label t = Ui.h3("Listas");
        HBox head = new HBox(8, t, Ui.spacer(), estadoProyectos.node());
        head.setAlignment(Pos.CENTER_LEFT);
        estadoProyectos.selectedProperty().addListener((o, a, b) -> fillProyectos());

        proyectos.getStyleClass().addAll("clean-list", "project-list");
        proyectos.setCellFactory(lv -> new ProyectoCell());
        proyectos.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> fillTareas());
        VBox.setVgrow(proyectos, Priority.ALWAYS);

        TextField nuevo = new TextField();
        nuevo.getStyleClass().add("bare-field");
        HBox add = new HBox(10, IconFactory.icon("add", 17), nuevo);
        add.getStyleClass().add("dashed-input");
        add.setAlignment(Pos.CENTER_LEFT);
        nuevo.setPromptText("Nueva lista y Enter");
        HBox.setHgrow(nuevo, Priority.ALWAYS);
        nuevo.setOnAction(e -> {
            String n = nuevo.getText().trim();
            if (n.isEmpty()) { Anim.shake(add); return; }
            String color = COLORES[dao.listar().size() % COLORES.length];
            if (dao.insertar(new Proyecto(0, n, "", "Activo", LocalDate.now().toString(), null, color))) {
                nuevo.clear();
                fillProyectos();
                Notifier.success("Lista creada", "«" + n + "» ya está en tu lista.");
            } else Notifier.error("No se pudo crear la lista", "Revisa la base de datos.");
        });

        VBox card = Ui.card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(18, 12, 14, 12));
        card.setPrefWidth(300);
        card.setMinWidth(250);
        card.setMaxWidth(320);
        VBox.setMargin(head, new Insets(0, 6, 0, 6));
        card.getChildren().addAll(head, proyectos, add);
        return card;
    }

    private final class ProyectoCell extends ListCell<Proyecto> {
        ProyectoCell() {
            setOnContextMenuRequested(e -> {
                Proyecto p = getItem();
                if (p != null && p != TODAS) menuProyecto(p).show(this, e.getScreenX(), e.getScreenY());
            });
        }

        @Override protected void updateItem(Proyecto p, boolean empty) {
            super.updateItem(p, empty);
            setText(null);
            if (empty || p == null) { setGraphic(null); return; }
            if (p == TODAS) {
                Label n = new Label(p.getNombre());
                n.getStyleClass().add("list-title");
                Label count = Ui.label(String.valueOf(c.getTareas().size()), "list-count");
                HBox row = new HBox(12, IconFactory.icon("inbox", 18, "tint-accent"), n, Ui.spacer(), count);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                return;
            }
            List<Tarea> ts = c.getTareas().stream().filter(t -> pertenece(t, p)).toList();
            long hechas = ts.stream().filter(Tarea::estaCompletada).count();
            int pct = ProgresoService.porcentaje(p.getId(), c.getTareas());
            StackPane dot = new StackPane();
            dot.getStyleClass().add("dot");
            dot.setStyle("-fx-background-color:" + safeColor(p.getColor()) + ";");
            Label n = new Label(p.getNombre());
            n.getStyleClass().add("list-title");
            HBox titleRow = new HBox(8, n);
            if (!"Activo".equalsIgnoreCase(p.getEstado())) titleRow.getChildren().add(Ui.pill(p.getEstado(), "slate"));
            titleRow.setAlignment(Pos.CENTER_LEFT);
            StackPane bar = Ui.bar(pct / 100.0, "accent", 4);
            VBox mid = new VBox(8, titleRow, bar);
            HBox.setHgrow(mid, Priority.ALWAYS);
            Label count = Ui.label(hechas + "/" + ts.size(), "list-count");
            HBox row = new HBox(12, dot, mid, count);
            row.setAlignment(Pos.CENTER_LEFT);
            bar.getChildren().getFirst().setStyle("-fx-background-color:" + safeColor(p.getColor()) + ";");
            Ui.tooltip(this, p.getNombre() + " · " + pct + "% completado · clic derecho para más opciones");
            setGraphic(row);
        }
    }

    private ContextMenu menuProyecto(Proyecto p) {
        Menu estado = new Menu("Cambiar estado");
        for (String e : new String[]{"Activo", "Pausado", "Completado"}) {
            MenuItem mi = new MenuItem(e);
            mi.setOnAction(x -> {
                p.setEstado(e);
                if (dao.actualizar(p)) { fillProyectos(); Notifier.success("Lista actualizada", p.getNombre() + " ahora está " + e.toLowerCase(Locale.ROOT) + "."); }
            });
            estado.getItems().add(mi);
        }
        MenuItem renombrar = new MenuItem("Renombrar…");
        renombrar.setOnAction(x -> {
            var d = new javafx.scene.control.TextInputDialog(p.getNombre());
            d.setTitle("Compás");
            d.setHeaderText("Nuevo nombre de la lista");
            d.getDialogPane().getStyleClass().add("cs-dialog");
            d.initOwner(view.getScene().getWindow());
            d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(s -> {
                p.setNombre(s);
                if (dao.actualizar(p)) { fillProyectos(); Notifier.success("Lista renombrada", s); }
            });
        });
        MenuItem borrar = new MenuItem("Eliminar lista");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(x -> {
            if (Ui.confirm(view, "¿Eliminar «" + p.getNombre() + "»?",
                "Sus tareas no se borran: quedarán sin lista.", "Eliminar")) {
                if (dao.eliminar(p.getId())) {
                    c.cargarTodo();
                    fillProyectos();
                    Notifier.deleted("Lista eliminada", "«" + p.getNombre() + "» se quitó de la lista.");
                }
            }
        });
        return new ContextMenu(estado, renombrar, new SeparatorMenuItem(), borrar);
    }

    private void fillProyectos() {
        Proyecto sel = proyectos.getSelectionModel().getSelectedItem();
        List<Proyecto> lista = new ArrayList<>();
        lista.add(TODAS);
        for (Proyecto p : dao.listar())
            if (estadoProyectos.selected() == 1 || !"Completado".equalsIgnoreCase(p.getEstado())) lista.add(p);
        proyectos.getItems().setAll(lista);
        if (sel != null) for (Proyecto p : lista) if (p.getId() == sel.getId()) { proyectos.getSelectionModel().select(p); break; }
        if (proyectos.getSelectionModel().getSelectedItem() == null) proyectos.getSelectionModel().selectFirst();
        fillTareas();
    }

    private static String safeColor(String c) {
        return c != null && c.matches("#[0-9a-fA-F]{6}") ? c : "#2F5BB7";
    }

    private static boolean pertenece(Tarea t, Proyecto p) {
        return t.getProyectoId() != null && t.getProyectoId() == p.getId();
    }

    // ------------------------------------------------------------------ tareas

    private VBox panelTareas() {
        HBox buscador = Ui.searchBox(buscar, "Buscar tareas");
        buscador.setPrefWidth(260);
        buscador.setMaxWidth(300);
        buscar.textProperty().addListener((o, a, b) -> fillTareas());
        filtro.selectedProperty().addListener((o, a, b) -> fillTareas());
        HBox toolbar = new HBox(12, filtro.node(), Ui.spacer(), buscador);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        grupos.getStyleClass().add("list-card");
        ScrollPane sp = new ScrollPane(grupos);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().addAll("card", "card-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox right = new VBox(16, toolbar, sp);
        HBox.setHgrow(right, Priority.ALWAYS);
        return right;
    }

    private void fillTareas() {
        Proyecto sel = proyectos.getSelectionModel().getSelectedItem();
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(Ui.ES);
        LocalDate hoy = LocalDate.now();
        List<Tarea> base = c.getTareas().stream()
            .filter(t -> sel == null || sel == TODAS || pertenece(t, sel))
            .filter(t -> q.isEmpty() || t.getTitulo().toLowerCase(Ui.ES).contains(q)
                || (t.getDescripcion() != null && t.getDescripcion().toLowerCase(Ui.ES).contains(q)))
            .toList();
        long pend = base.stream().filter(t -> !t.estaCompletada()).count();
        long nHoy = base.stream().filter(t -> !t.estaCompletada() && hoy.equals(t.getFecha())).count();
        long nProx = base.stream().filter(t -> !t.estaCompletada() && t.getFecha() != null && t.getFecha().isAfter(hoy)).count();
        long nVenc = base.stream().filter(t -> !t.estaCompletada() && t.getFecha() != null && t.getFecha().isBefore(hoy)).count();
        long nHechas = base.size() - pend;
        filtro.setText(0, "Todas  " + pend);
        filtro.setText(1, "Hoy  " + nHoy);
        filtro.setText(2, "Próximas  " + nProx);
        filtro.setText(3, "Vencidas  " + nVenc);
        filtro.setText(4, "Completadas  " + nHechas);

        Comparator<Tarea> orden = Comparator.comparing((Tarea t) -> t.getFecha() == null ? LocalDate.MAX : t.getFecha())
            .thenComparing(t -> -t.getPrioridadTipo().ordinal());
        Map<String, List<Tarea>> grupo = new LinkedHashMap<>();
        int f = filtro.selected();
        LocalDate finSemana = hoy.with(DayOfWeek.SUNDAY);
        for (Tarea t : base.stream().sorted(orden).toList()) {
            LocalDate d = t.getFecha();
            if (t.estaCompletada()) { if (f == 0 || f == 4) grupo.computeIfAbsent("Completadas", k -> new ArrayList<>()).add(t); continue; }
            if (f == 4) continue;
            String g;
            if (d == null) g = "Sin fecha";
            else if (d.isBefore(hoy)) g = "Vencidas";
            else if (d.equals(hoy)) g = "Hoy";
            else if (d.equals(hoy.plusDays(1))) g = "Mañana";
            else if (!d.isAfter(finSemana)) g = "Esta semana";
            else g = "Más adelante";
            if (f == 1 && !g.equals("Hoy")) continue;
            if (f == 2 && (g.equals("Hoy") || g.equals("Vencidas"))) continue;
            if (f == 3 && !g.equals("Vencidas")) continue;
            grupo.computeIfAbsent(g, k -> new ArrayList<>()).add(t);
        }
        // Orden de los grupos: vencidas primero, completadas al final.
        List<String> ordenGrupos = List.of("Vencidas", "Hoy", "Mañana", "Esta semana", "Más adelante", "Sin fecha", "Completadas");
        grupos.getChildren().clear();
        if (grupo.isEmpty()) {
            String msg = q.isEmpty() ? (f == 4 ? "Aún no completas tareas aquí." : "No tienes pendientes en esta vista. ¡Buen trabajo!")
                : "Ninguna tarea coincide con «" + q + "».";
            grupos.getChildren().add(Ui.empty("tasks", "Nada por aquí", msg));
            return;
        }
        List<Node> filas = new ArrayList<>();
        boolean primero = true;
        for (String g : ordenGrupos) {
            List<Tarea> ts = grupo.get(g);
            if (ts == null) continue;
            HBox head = new HBox(8, Ui.overline(g), Ui.label(String.valueOf(ts.size()), "count-pill"));
            head.setAlignment(Pos.CENTER_LEFT);
            head.getStyleClass().add("group-head");
            if (g.equals("Vencidas")) head.getStyleClass().add("group-head-danger");
            if (!primero) head.getStyleClass().add("with-divider");
            primero = false;
            grupos.getChildren().add(head);
            List<Tarea> visibles = ts;
            if (g.equals("Completadas") && f == 0 && !verCompletadas && ts.size() > 2) visibles = ts.subList(0, 2);
            for (Tarea t : visibles) { Node r = fila(t); grupos.getChildren().add(r); filas.add(r); }
            if (visibles.size() < ts.size()) {
                Button mas = Ui.ghost("chevron-down", "Ver " + (ts.size() - visibles.size()) + " completadas más");
                mas.setOnAction(e -> { verCompletadas = true; fillTareas(); });
                HBox w = new HBox(mas);
                w.setPadding(new Insets(4, 16, 12, 16));
                grupos.getChildren().add(w);
            } else if (g.equals("Completadas") && verCompletadas && f == 0 && ts.size() > 2) {
                Button menos = Ui.ghost("chevron-up", "Ocultar completadas");
                menos.setOnAction(e -> { verCompletadas = false; fillTareas(); });
                HBox w = new HBox(menos);
                w.setPadding(new Insets(4, 16, 12, 16));
                grupos.getChildren().add(w);
            }
        }
        Anim.stagger(filas);
    }

    private Node fila(Tarea t) {
        boolean hecha = t.estaCompletada();
        Button check = new Button();
        check.getStyleClass().addAll("check-circle");
        if (hecha) check.getStyleClass().add("checked");
        check.setGraphic(IconFactory.icon("check", 14, "tint-on-accent"));
        check.setAccessibleText(hecha ? "Marcar como pendiente" : "Completar tarea");
        Ui.tooltip(check, hecha ? "Marcar como pendiente" : "Completar");
        check.setOnAction(e -> {
            Anim.pop(check);
            c.alternarCompletada(t).ifPresentOrElse(r -> {
                if (r.completada()) Notifier.success(c.mensajeProgreso(r), t.getTitulo());
                else Notifier.info(c.mensajeProgreso(r), t.getTitulo());
            }, () -> Notifier.error("No se pudo actualizar la tarea", null));
        });

        Label titulo = new Label(t.getTitulo());
        titulo.getStyleClass().add("row-title");
        if (hecha) titulo.getStyleClass().add("done");
        // Viñeta de importancia (rojo = alta, naranja = media, verde = baja) para distinguir de un vistazo.
        StackPane vineta = new StackPane();
        vineta.getStyleClass().addAll("dot");
        vineta.setStyle("-fx-background-color: -" + t.getPrioridadTipo().tono() + ";");
        Ui.tooltip(titulo, "Importancia " + t.getPrioridad().toLowerCase());
        titulo.setGraphic(vineta);
        titulo.setGraphicTextGap(8);
        HBox meta = new HBox(14);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (t.getProyectoId() != null) {
            for (Proyecto p : proyectos.getItems()) if (p.getId() == t.getProyectoId()) {
                StackPane dot = new StackPane();
                dot.getStyleClass().addAll("dot", "dot-small");
                dot.setStyle("-fx-background-color:" + safeColor(p.getColor()) + ";");
                Label n = Ui.label(p.getNombre(), "row-meta");
                n.setGraphic(dot);
                n.setGraphicTextGap(6);
                meta.getChildren().add(n);
            }
        }
        Label fecha = Ui.label(hecha ? "Completada" : Ui.friendlyDate(t.getFecha()), "row-meta");
        fecha.setGraphic(IconFactory.icon("calendar", 13));
        fecha.setGraphicTextGap(6);
        if (!hecha && t.getFecha() != null && t.getFecha().isBefore(LocalDate.now())) fecha.getStyleClass().add("text-danger");
        if (t.getHora() != null) fecha.setText(fecha.getText() + " · " + t.getHora());
        meta.getChildren().add(fecha);
        if (t.getEventoId() != null) meta.getChildren().add(Ui.label("Evento vinculado", "row-meta"));
        if (t.getBloqueId() != null) meta.getChildren().add(Ui.label("Bloque vinculado", "row-meta"));
        if (t.getEstadoTipo() == EstadoTarea.EN_PROGRESO) meta.getChildren().add(Ui.pill("En progreso", "blue"));
        VBox texts = new VBox(5, titulo, meta);
        HBox.setHgrow(texts, Priority.ALWAYS);

        String tono = t.getPrioridadTipo().tono();
        Label pri = Ui.pill(t.getPrioridad(), tono);
        Button mas = Ui.iconButton("more", "Más opciones");
        mas.setOnAction(e -> menuTarea(t).show(mas, javafx.geometry.Side.BOTTOM, 0, 4));

        Button estrella = Ui.iconButton(t.isDestacada() ? "favorite-on" : "favorite", t.isDestacada() ? "Quitar destacado" : "Destacar");
        estrella.setOnAction(e -> { t.setDestacada(!t.isDestacada()); c.actualizarTarea(t); });
        HBox row = new HBox(12, check, texts, pri, estrella, mas);
        row.getStyleClass().add("list-row");
        if (hecha) row.getStyleClass().add("row-done");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> { if (e.getClickCount() == 2) TaskViews.form(t, c, this::fillTareas); });
        return row;
    }

    private ContextMenu menuTarea(Tarea t) {
        MenuItem editar = new MenuItem("Editar…");
        editar.setOnAction(e -> TaskViews.form(t, c, this::fillTareas));
        Menu estado = new Menu("Estado");
        for (EstadoTarea es : EstadoTarea.values()) {
            MenuItem mi = new MenuItem(es.etiqueta());
            mi.setDisable(es == t.getEstadoTipo());
            mi.setOnAction(e -> c.cambiarEstadoTarea(t, es).ifPresent(r ->
                Notifier.success("Estado actualizado", t.getTitulo() + " → " + es.etiqueta())));
            estado.getItems().add(mi);
        }
        MenuItem borrar = new MenuItem("Eliminar");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (Ui.confirm(view, "¿Eliminar esta tarea?", "«" + t.getTitulo() + "» se borrará para siempre.", "Eliminar")
                && c.eliminarTarea(t.getId()))
                Notifier.deleted("Tarea eliminada", t.getTitulo());
        });
        return new ContextMenu(editar, estado, new SeparatorMenuItem(), borrar);
    }

    public VBox getView() { return view; }
}
