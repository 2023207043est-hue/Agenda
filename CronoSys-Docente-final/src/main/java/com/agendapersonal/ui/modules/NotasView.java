package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.infrastructure.repo.NotaDAO;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** Notas e ideas: captura rápida arriba, lista con filtros a la izquierda y editor a la derecha. */
public class NotasView {
    private final NotaDAO dao = new NotaDAO();
    private final VBox view;
    private final ListView<Nota> list = new ListView<>();
    private final TextField titulo = new TextField();
    private final TextArea contenido = new TextArea();
    private final TextField etiquetas = new TextField();
    private final ComboBox<String> tipo = new ComboBox<>();
    private final Button fav = Ui.iconButton("favorite", "Marcar como favorita");
    private final Label estado = Ui.label("", "caption");
    private final TextField buscar = new TextField();
    private final Ui.Segmented filtro = new Ui.Segmented("Todas", "Notas", "Ideas", "Favoritas");
    private boolean favorita;
    private boolean cargando;
    private boolean sucio;
    private Nota actual;

    public NotasView() { this(null); }

    public NotasView(String filtroInicial) {
        VBox page = Ui.page();
        Button nueva = Ui.primary("add", "Nueva nota");
        nueva.setOnAction(e -> nueva());
        page.getChildren().add(Ui.pageHeader("notes", "Notas",
            "Captura ideas al vuelo y encuéntralas con etiquetas.", nueva));

        // Captura rápida de idea
        TextField idea = new TextField();
        idea.getStyleClass().add("bare-field");
        idea.setPromptText("Captura una idea y pulsa Enter…");
        HBox.setHgrow(idea, Priority.ALWAYS);
        Label enter = Ui.label("Enter", "kbd");
        HBox captura = new HBox(12, IconFactory.icon("idea", 20, "tint-amber"), idea, enter);
        captura.getStyleClass().add("capture-bar");
        captura.setAlignment(Pos.CENTER_LEFT);
        captura.setOnMouseClicked(e -> idea.requestFocus());
        idea.setOnAction(e -> {
            String txt = idea.getText().trim();
            if (txt.isEmpty()) { Anim.shake(captura); return; }
            String hoy = LocalDate.now().toString();
            if (dao.insertar(new Nota(0, txt, "", "idea", "IDEA", hoy, hoy, false))) {
                idea.clear();
                fill();
                Notifier.success("Idea guardada", txt);
            } else Notifier.error("No se pudo guardar la idea", null);
        });

        HBox body = new HBox(24, panelLista(filtroInicial), panelEditor());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().addAll(captura, body);
        view = page;
        fill();
        list.getSelectionModel().selectFirst();
        if (list.getItems().isEmpty()) nueva();
    }

    private VBox panelLista(String filtroInicial) {
        HBox busq = Ui.searchBox(buscar, "Buscar notas o etiquetas");
        busq.getStyleClass().add("search-soft");
        if (filtroInicial != null) buscar.setText(filtroInicial);
        buscar.textProperty().addListener((o, a, b) -> fill());
        filtro.selectedProperty().addListener((o, a, b) -> fill());
        filtro.fillWidth();
        list.getStyleClass().addAll("clean-list", "note-list");
        list.setCellFactory(lv -> new NotaCell());
        list.setPlaceholder(Ui.empty("notes", "Sin notas", "Escribe una idea arriba o crea una nota nueva."));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null) mostrar(b); });
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox card = Ui.card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(16, 10, 10, 10));
        card.setPrefWidth(340);
        card.setMinWidth(270);
        card.setMaxWidth(380);
        VBox top = new VBox(12, busq, filtro.node());
        top.setPadding(new Insets(0, 6, 4, 6));
        card.getChildren().addAll(top, list);
        return card;
    }

    private final class NotaCell extends ListCell<Nota> {
        @Override protected void updateItem(Nota n, boolean empty) {
            super.updateItem(n, empty);
            setText(null);
            if (empty || n == null) { setGraphic(null); return; }
            Label t = new Label(n.getTitulo());
            t.getStyleClass().add("list-title");
            HBox head = new HBox(6, t);
            head.setAlignment(Pos.CENTER_LEFT);
            if (n.isFavorita()) { Region sp = Ui.spacer(); head.getChildren().addAll(sp, IconFactory.icon("favorite", 15, "tint-amber", "icon-fill")); }
            String prev = n.getContenido() == null ? "" : n.getContenido().replace('\n', ' ').trim();
            Label p = new Label(prev.isEmpty() ? "Sin contenido todavía" : prev);
            p.getStyleClass().add("list-sub");
            HBox tags = new HBox(6);
            tags.setAlignment(Pos.CENTER_LEFT);
            tags.getChildren().add(Ui.pill("IDEA".equalsIgnoreCase(n.getTipo()) ? "idea" : "nota",
                "IDEA".equalsIgnoreCase(n.getTipo()) ? "amber" : "slate"));
            for (String tag : etiquetasDe(n).stream().limit(2).toList()) tags.getChildren().add(Ui.pill(tag, "slate"));
            Label fecha = Ui.label(cuando(n), "caption");
            HBox foot = new HBox(6, tags, Ui.spacer(), fecha);
            foot.setAlignment(Pos.CENTER_LEFT);
            VBox box = new VBox(6, head, p, foot);
            box.setMaxWidth(Double.MAX_VALUE);
            p.setMaxWidth(290);
            setGraphic(box);
        }
    }

    private static List<String> etiquetasDe(Nota n) {
        if (n.getEtiquetas() == null || n.getEtiquetas().isBlank()) return List.of();
        return java.util.Arrays.stream(n.getEtiquetas().split(","))
            .map(String::trim).filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("idea")).toList();
    }

    private static String cuando(Nota n) {
        String f = n.getFechaUpdate() != null ? n.getFechaUpdate() : n.getFechaCreacion();
        try { return Ui.friendlyDate(LocalDate.parse(f)); } catch (Exception e) { return ""; }
    }

    private VBox panelEditor() {
        tipo.getItems().addAll("NOTA", "IDEA");
        tipo.setValue("NOTA");
        tipo.getStyleClass().add("compact-combo");
        fav.setOnAction(e -> { setFavorita(!favorita); Anim.pop(fav); marcarSucio(); });
        HBox top = new HBox(10, tipo, estado, Ui.spacer(), fav);
        top.setAlignment(Pos.CENTER_LEFT);

        titulo.setPromptText("Título de la nota");
        titulo.getStyleClass().addAll("bare-field", "title-field");
        HBox tagRow = new HBox(10, IconFactory.icon("tag", 16), etiquetas);
        tagRow.setAlignment(Pos.CENTER_LEFT);
        etiquetas.setPromptText("etiquetas separadas por coma (trabajo, libros…)");
        etiquetas.getStyleClass().add("bare-field");
        HBox.setHgrow(etiquetas, Priority.ALWAYS);
        Region linea = new Region();
        linea.getStyleClass().add("hairline");

        contenido.setPromptText("Escribe aquí tus ideas, enlaces, listas…");
        contenido.setWrapText(true);
        contenido.getStyleClass().add("editor-area");
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Button borrar = Ui.ghost("delete", "Eliminar");
        borrar.getStyleClass().add("text-danger-btn");
        borrar.setOnAction(e -> borrar());
        Button guardar = Ui.primary("check", "Guardar");
        guardar.setOnAction(e -> guardar());
        Label atajo = Ui.label("Ctrl+S", "kbd");
        HBox foot = new HBox(10, borrar, Ui.spacer(), atajo, guardar);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getStyleClass().add("card-footer");

        for (var tf : List.of(titulo.textProperty(), contenido.textProperty(), etiquetas.textProperty()))
            tf.addListener((o, a, b) -> marcarSucio());
        tipo.setOnAction(e -> marcarSucio());

        VBox card = Ui.card();
        card.getStyleClass().add("editor-card");
        card.setPadding(new Insets(20, 28, 18, 28));
        card.setSpacing(14);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(top, titulo, tagRow, linea, contenido, foot);
        card.sceneProperty().addListener((o, a, s) -> {
            if (s != null) s.getAccelerators().putIfAbsent(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {
                if (card.getScene() != null && card.isVisible() && card.getParent() != null && view.getParent() != null) guardar();
            });
        });
        return card;
    }

    private void setFavorita(boolean on) {
        favorita = on;
        fav.setGraphic(IconFactory.icon("favorite", 18, on ? "tint-amber" : "tint-none", on ? "icon-fill" : "no-fill"));
        fav.getStyleClass().removeAll("fav-on");
        if (on) fav.getStyleClass().add("fav-on");
        Ui.tooltip(fav, on ? "Quitar de favoritas" : "Marcar como favorita");
    }

    private void marcarSucio() {
        if (cargando) return;
        sucio = true;
        estado.setText("Cambios sin guardar");
        estado.getStyleClass().setAll("caption", "text-warning");
    }

    private void nueva() {
        cargando = true;
        actual = null;
        list.getSelectionModel().clearSelection();
        titulo.clear(); contenido.clear(); etiquetas.clear();
        tipo.setValue("NOTA");
        setFavorita(false);
        cargando = false;
        sucio = false;
        estado.setText("Nota nueva");
        estado.getStyleClass().setAll("caption");
        titulo.requestFocus();
    }

    private void guardar() {
        if (titulo.getText().isBlank()) { Anim.shake(titulo); Notifier.warning("Falta el título", "Ponle un título a la nota para guardarla."); return; }
        String hoy = LocalDate.now().toString();
        boolean ok;
        boolean esNueva = actual == null;
        if (esNueva) {
            ok = dao.insertar(new Nota(0, titulo.getText().trim(), contenido.getText(), etiquetas.getText(),
                tipo.getValue(), hoy, hoy, favorita));
        } else {
            actual.setTitulo(titulo.getText().trim());
            actual.setContenido(contenido.getText());
            actual.setEtiquetas(etiquetas.getText());
            actual.setTipo(tipo.getValue());
            actual.setFechaUpdate(hoy);
            actual.setFavorita(favorita);
            ok = dao.actualizar(actual);
        }
        if (!ok) { Notifier.error("No se pudo guardar la nota", null); return; }
        String t = titulo.getText().trim();
        fill();
        // vuelve a seleccionar la nota guardada
        for (Nota n : list.getItems()) if (n.getTitulo().equals(t)) { list.getSelectionModel().select(n); break; }
        sucio = false;
        estado.setText("Guardado");
        estado.getStyleClass().setAll("caption", "text-success");
        Notifier.success(esNueva ? "Nota creada" : "Nota guardada", t);
    }

    private void borrar() {
        if (actual == null) { nueva(); return; }
        if (!Ui.confirm(view, "¿Eliminar esta nota?", "«" + actual.getTitulo() + "» se borrará para siempre.", "Eliminar")) return;
        String t = actual.getTitulo();
        if (dao.eliminar(actual.getId())) {
            fill();
            nueva();
            list.getSelectionModel().selectFirst();
            Notifier.deleted("Nota eliminada", t);
        }
    }

    private void fill() {
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(Ui.ES);
        int f = filtro.selected();
        List<Nota> todas = dao.listar(null);
        Nota sel = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(todas.stream()
            .filter(n -> f == 0 || (f == 1 && !"IDEA".equalsIgnoreCase(n.getTipo()))
                || (f == 2 && "IDEA".equalsIgnoreCase(n.getTipo())) || (f == 3 && n.isFavorita()))
            .filter(n -> q.isEmpty() || contiene(n.getTitulo(), q) || contiene(n.getContenido(), q) || contiene(n.getEtiquetas(), q))
            .toList());
        if (sel != null) for (Nota n : list.getItems()) if (n.getId() == sel.getId()) { list.getSelectionModel().select(n); break; }
    }

    private static boolean contiene(String s, String q) { return s != null && s.toLowerCase(Ui.ES).contains(q); }

    private void mostrar(Nota n) {
        if (sucio && actual != null && actual.getId() != n.getId())
            Notifier.warning("Había cambios sin guardar", "Se descartaron los cambios de «" + actual.getTitulo() + "».");
        cargando = true;
        actual = n;
        titulo.setText(n.getTitulo());
        contenido.setText(n.getContenido() == null ? "" : n.getContenido());
        etiquetas.setText(n.getEtiquetas() == null ? "" : n.getEtiquetas());
        tipo.setValue(n.getTipo() == null ? "NOTA" : n.getTipo().toUpperCase(Ui.ES));
        setFavorita(n.isFavorita());
        cargando = false;
        sucio = false;
        estado.setText("Editado " + cuando(n).toLowerCase(Ui.ES));
        estado.getStyleClass().setAll("caption");
    }

    public VBox getView() { return view; }
}
