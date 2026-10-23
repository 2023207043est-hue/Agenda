package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.infrastructure.repo.ContactoDAO;
import com.agendapersonal.service.ContactoService;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Contactos (CRM personal): lista con avatares a la izquierda y ficha editable a la derecha. */
public class CrmView {
    private static final int DIAS_SEGUIMIENTO = 30;
    private final ContactoDAO dao = new ContactoDAO();
    private final VBox view;
    private final ListView<Contacto> list = new ListView<>();
    private final TextField buscar = new TextField();
    private final Ui.Segmented filtro = new Ui.Segmented("Todos", "Favoritos", "Por contactar");
    private final TextField nombre = new TextField();
    private final TextField tel = new TextField();
    private final TextField email = new TextField();
    private final ComboBox<String> categoria = new ComboBox<>();
    private final DatePicker cumple = new DatePicker();
    private final TextField apodo = new TextField();
    private final VBox fechasBox = new VBox(8);
    private final Label faltan = Ui.label("", "pill", "pill-violet");
    private final TextArea notas = new TextArea();
    private final StackPane avatarBox = new StackPane();
    private final Label nombreGrande = Ui.label("", "h2");
    private final HBox chips = new HBox(10);
    private final Button favBtn = Ui.iconButton("favorite", "Marcar como favorito");
    private final Button contactado = Ui.secondary("check", "Contactado hoy");
    private final Button borrar = Ui.ghost("delete", "Eliminar contacto");
    private boolean favorito;
    private Contacto actual;

    public CrmView() {
        VBox page = Ui.page();
        Button nuevo = Ui.primary("add", "Nuevo contacto");
        nuevo.setOnAction(e -> limpiar());
        page.getChildren().add(Ui.pageHeader("contacts", "Contactos",
            "Tus personas importantes y cuándo hablaste con ellas por última vez.", nuevo));
        HBox body = new HBox(24, panelLista(), panelFicha());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().add(body);
        view = page;
        fill();
        list.getSelectionModel().selectFirst();
        if (list.getItems().isEmpty()) limpiar();
    }

    private VBox panelLista() {
        HBox busq = Ui.searchBox(buscar, "Buscar contactos");
        busq.getStyleClass().add("search-soft");
        buscar.textProperty().addListener((o, a, b) -> fill());
        filtro.selectedProperty().addListener((o, a, b) -> fill());
        filtro.fillWidth();
        list.getStyleClass().addAll("clean-list", "contact-list");
        list.setCellFactory(lv -> new ContactoCell());
        list.setPlaceholder(Ui.empty("contacts", "Sin contactos", "Agrega a las personas importantes con «Nuevo contacto»."));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null) mostrar(b); });
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox top = new VBox(12, busq, filtro.node());
        top.setPadding(new Insets(0, 6, 4, 6));
        VBox card = Ui.card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(16, 10, 10, 10));
        card.setPrefWidth(340);
        card.setMinWidth(270);
        card.setMaxWidth(380);
        card.getChildren().addAll(top, list);
        return card;
    }

    private final class ContactoCell extends ListCell<Contacto> {
        @Override protected void updateItem(Contacto x, boolean empty) {
            super.updateItem(x, empty);
            setText(null);
            if (empty || x == null) { setGraphic(null); return; }
            Label n = new Label(x.getNombre());
            n.getStyleClass().add("list-title");
            String cat = x.getCategoria() == null ? "Personal" : x.getCategoria();
            Label sub = new Label(cat + " · " + Ui.haceCuanto(ultimo(x)));
            sub.getStyleClass().add("list-sub");
            VBox t = new VBox(3, n, sub);
            HBox.setHgrow(t, Priority.ALWAYS);
            HBox row = new HBox(12, Ui.avatar(x.getNombre(), 40), t);
            row.setAlignment(Pos.CENTER_LEFT);
            if (necesitaSeguimiento(x)) row.getChildren().add(Ui.pill("Escríbele", "amber"));
            else if (x.isFavorito()) row.getChildren().add(IconFactory.icon("favorite", 16, "tint-amber", "icon-fill"));
            setGraphic(row);
        }
    }

    private static LocalDate ultimo(Contacto x) {
        try { return x.getUltimoContacto() == null || x.getUltimoContacto().isBlank() ? null : LocalDate.parse(x.getUltimoContacto()); }
        catch (Exception e) { return null; }
    }

    private static boolean necesitaSeguimiento(Contacto x) {
        OptionalLong d = ContactoService.diasSinContacto(x, LocalDate.now());
        return d.isPresent() && d.getAsLong() > DIAS_SEGUIMIENTO;
    }

    private Node panelFicha() {
        avatarBox.setMinSize(64, 64);
        HBox actions = new HBox(8, contactado, favBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox nameBox = new VBox(8, nombreGrande, chips);
        chips.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(16, avatarBox, nameBox, Ui.spacer(), actions);
        header.setAlignment(Pos.CENTER_LEFT);

        categoria.getItems().addAll("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        for (String extra : com.agendapersonal.settings.SettingsStore.get().strings("contacts.categories"))
            if (!categoria.getItems().contains(extra)) categoria.getItems().add(extra);
        categoria.setEditable(true);          // se puede escribir una categoría propia
        categoria.setValue("Personal");
        apodo.setPromptText("Cómo le dices (opcional)");
        nombre.setPromptText("Nombre y apellido");
        tel.setPromptText("+51 999 999 999");
        email.setPromptText("correo@ejemplo.com");
        Ui.fechaCorta(cumple);
        cumple.setPromptText("Elige la fecha");
        cumple.valueProperty().addListener((o, a, b) -> actualizarFaltan());
        notas.setPromptText("Contexto, gustos, de qué hablaron la última vez…");
        notas.setWrapText(true);
        notas.setPrefRowCount(5);

        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(16);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        form.getColumnConstraints().addAll(col, col);
        form.add(Ui.field("Nombre", nombre), 0, 0);
        form.add(Ui.field("Categoría", categoria), 1, 0);
        form.add(Ui.field("Teléfono", conIcono("phone", tel)), 0, 1);
        form.add(Ui.field("Correo", conIcono("mail", email)), 1, 1);
        HBox cumpleRow = new HBox(10, cumple, faltan);
        cumpleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cumple, Priority.ALWAYS);
        cumple.setMaxWidth(Double.MAX_VALUE);
        form.add(Ui.field("Cumpleaños", cumpleRow), 0, 2);
        form.add(Ui.field("Apodo", apodo), 1, 2);
        Button masFecha = Ui.secondary(null, "+ Agregar fecha importante");
        masFecha.setOnAction(e -> filaFecha("", null));
        form.add(Ui.field("Otras fechas importantes (aniversario, graduación…)", new VBox(8, fechasBox, masFecha)), 0, 3, 2, 1);
        VBox notasField = Ui.field("Notas", notas);
        VBox.setVgrow(notas, Priority.ALWAYS);

        contactado.setOnAction(e -> {
            if (actual == null) return;
            ContactoService.marcarContactadoHoy(actual, LocalDate.now());
            if (dao.actualizar(actual)) {
                Anim.pop(contactado);
                Notifier.success("Último contacto: hoy", "Registraste que hablaste con " + actual.getNombre() + ".");
                fill();
                mostrar(actual);
            }
        });
        favBtn.setOnAction(e -> { setFav(!favorito); Anim.pop(favBtn); });
        borrar.getStyleClass().add("text-danger-btn");
        borrar.setOnAction(e -> eliminar());
        Button cancelar = Ui.secondary(null, "Cancelar");
        cancelar.setOnAction(e -> { if (actual != null) mostrar(actual); else limpiar(); });
        Button guardar = Ui.primary("check", "Guardar cambios");
        guardar.setOnAction(e -> guardar());
        HBox foot = new HBox(10, borrar, Ui.spacer(), cancelar, guardar);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getStyleClass().add("card-footer");

        Region linea = new Region();
        linea.getStyleClass().add("hairline");
        VBox card = Ui.card();
        card.setPadding(new Insets(24, 28, 18, 28));
        card.setSpacing(20);
        card.getChildren().addAll(header, linea, form, notasField, foot);
        ScrollPane sp = new ScrollPane(card);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("page-scroll");
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private static HBox conIcono(String icon, TextField f) {
        f.getStyleClass().add("bare-field");
        HBox.setHgrow(f, Priority.ALWAYS);
        HBox box = new HBox(10, IconFactory.icon(icon, 16), f);
        box.getStyleClass().add("input-with-icon");
        box.setAlignment(Pos.CENTER_LEFT);
        f.focusedProperty().addListener((o, a, b) ->
            box.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), b));
        return box;
    }

    private void setFav(boolean on) {
        favorito = on;
        favBtn.setGraphic(IconFactory.icon("favorite", 18, on ? "tint-amber" : "tint-none", on ? "icon-fill" : "no-fill"));
        favBtn.getStyleClass().remove("fav-on");
        if (on) favBtn.getStyleClass().add("fav-on");
        Ui.tooltip(favBtn, on ? "Quitar de favoritos" : "Marcar como favorito");
    }

    private void actualizarFaltan() {
        LocalDate d = cumple.getValue();
        if (d == null) { faltan.setVisible(false); faltan.setManaged(false); return; }
        LocalDate hoy = LocalDate.now();
        LocalDate prox = d.withYear(hoy.getYear());
        if (prox.isBefore(hoy)) prox = prox.plusYears(1);
        long dias = ChronoUnit.DAYS.between(hoy, prox);
        faltan.setText(dias == 0 ? "¡Es hoy!" : dias == 1 ? "Mañana" : "Faltan " + dias + " días");
        faltan.setVisible(true);
        faltan.setManaged(true);
    }

    private void cabecera(String n, String cat, LocalDate ult, boolean nuevo) {
        avatarBox.getChildren().setAll(nuevo ? Ui.iconChip("contacts", "slate", 64, 28) : Ui.avatar(n, 64));
        nombreGrande.setText(nuevo ? "Nuevo contacto" : n);
        chips.getChildren().clear();
        if (!nuevo) {
            chips.getChildren().add(Ui.pill(cat, "blue"));
            Label u = Ui.label(ult == null ? "Aún sin registrar contacto" : "Último contacto " + Ui.haceCuanto(ult), "muted");
            u.setGraphic(IconFactory.icon("clock", 14));
            u.setGraphicTextGap(6);
            chips.getChildren().add(u);
        } else chips.getChildren().add(Ui.muted("Completa los datos y pulsa «Guardar cambios»."));
        contactado.setDisable(nuevo);
        borrar.setDisable(nuevo);
    }

    private void fill() {
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(Ui.ES);
        int f = filtro.selected();
        List<Contacto> todos = dao.listar();
        Contacto sel = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(todos.stream()
            .filter(x -> f == 0 || (f == 1 && x.isFavorito()) || (f == 2 && necesitaSeguimiento(x)))
            .filter(x -> q.isEmpty() || x.getNombre().toLowerCase(Ui.ES).contains(q)
                || (x.getEmail() != null && x.getEmail().toLowerCase(Ui.ES).contains(q))
                || (x.getTelefono() != null && x.getTelefono().contains(q)))
            .toList());
        if (sel != null) for (Contacto x : list.getItems()) if (x.getId() == sel.getId()) { list.getSelectionModel().select(x); break; }
    }

    private void mostrar(Contacto x) {
        actual = x;
        nombre.setText(x.getNombre());
        tel.setText(x.getTelefono() == null ? "" : x.getTelefono());
        email.setText(x.getEmail() == null ? "" : x.getEmail());
        String catX = x.getCategoria() == null ? "Personal" : x.getCategoria();
        if (!categoria.getItems().contains(catX)) categoria.getItems().add(catX);
        categoria.setValue(catX);
        apodo.setText(x.getApodo() == null ? "" : x.getApodo());
        fechasBox.getChildren().clear();
        for (var f : com.agendapersonal.service.FechasImportantes.leer(x.getFechas())) filaFecha(f.etiqueta(), f.fecha());
        try { cumple.setValue(x.getCumpleanos() == null || x.getCumpleanos().isBlank() ? null : LocalDate.parse(x.getCumpleanos())); }
        catch (Exception ignored) { cumple.setValue(null); }
        // Antes el "Último contacto" se pegaba dentro de las notas y se duplicaba en cada guardado.
        notas.setText(x.getNotas() == null ? "" : x.getNotas());
        setFav(x.isFavorito());
        cabecera(x.getNombre(), categoria.getValue(), ultimo(x), false);
        actualizarFaltan();
    }

    private void limpiar() {
        actual = null;
        list.getSelectionModel().clearSelection();
        nombre.clear(); tel.clear(); email.clear(); notas.clear();
        categoria.setValue("Personal");
        cumple.setValue(null);
        apodo.clear();
        fechasBox.getChildren().clear();
        setFav(false);
        cabecera(null, null, null, true);
        actualizarFaltan();
        nombre.requestFocus();
    }

    /** Agrega una fila (etiqueta + fecha + quitar) a las fechas importantes del contacto. */
    private void filaFecha(String etiqueta, LocalDate fecha) {
        TextField et = new TextField(etiqueta);
        et.setPromptText("Ej. Aniversario");
        DatePicker dp = new DatePicker(fecha);
        Ui.fechaCorta(dp);
        Button quitar = Ui.iconButton("close", "Quitar fecha");
        HBox fila = new HBox(8, et, dp, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(et, Priority.ALWAYS);
        quitar.setOnAction(e -> fechasBox.getChildren().remove(fila));
        fechasBox.getChildren().add(fila);
    }

    private String leerFechas() {
        List<com.agendapersonal.service.FechasImportantes.Fecha> l = new ArrayList<>();
        for (Node n : fechasBox.getChildren())
            if (n instanceof HBox h && h.getChildren().get(0) instanceof TextField t && h.getChildren().get(1) instanceof DatePicker d && d.getValue() != null)
                l.add(new com.agendapersonal.service.FechasImportantes.Fecha(t.getText(), d.getValue()));
        return com.agendapersonal.service.FechasImportantes.escribir(l);
    }

    /** Recuerda las categorías propias para ofrecerlas la próxima vez. */
    private void guardarCategoria(String cat) {
        var st = com.agendapersonal.settings.SettingsStore.get();
        List<String> base = List.of("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        List<String> propias = new ArrayList<>(st.strings("contacts.categories"));
        if (!base.contains(cat) && !propias.contains(cat)) { propias.add(cat); st.set("contacts.categories", propias); }
        if (!categoria.getItems().contains(cat)) categoria.getItems().add(cat);
    }

    private void guardar() {
        if (nombre.getText().isBlank()) {
            Anim.shake(nombre);
            Notifier.warning("Falta el nombre", "Escribe el nombre del contacto para guardarlo.");
            return;
        }
        String cum = cumple.getValue() == null ? null : cumple.getValue().toString();
        String n = nombre.getText().trim();
        String cat = categoria.getEditor().getText() == null || categoria.getEditor().getText().isBlank() ? "Personal" : categoria.getEditor().getText().trim();
        categoria.setValue(cat);
        guardarCategoria(cat);
        String fechasTxt = leerFechas();
        boolean ok;
        boolean esNuevo = actual == null;
        if (esNuevo) {
            Contacto nuevo = new Contacto(0, n, tel.getText().trim(), email.getText().trim(),
                cat, cum, notas.getText(), favorito, null);
            nuevo.setApodo(apodo.getText().trim());
            nuevo.setFechas(fechasTxt);
            ok = dao.insertar(nuevo);
        } else {
            actual.setNombre(n);
            actual.setTelefono(tel.getText().trim());
            actual.setEmail(email.getText().trim());
            actual.setCategoria(cat);
            actual.setApodo(apodo.getText().trim());
            actual.setFechas(fechasTxt);
            actual.setCumpleanos(cum);
            actual.setNotas(notas.getText());
            actual.setFavorito(favorito);
            ok = dao.actualizar(actual);
        }
        if (!ok) { Notifier.error("No se pudo guardar el contacto", null); return; }
        fill();
        for (Contacto x : list.getItems()) if (x.getNombre().equals(n)) { list.getSelectionModel().select(x); mostrar(x); break; }
        Notifier.success(esNuevo ? "Contacto agregado" : "Contacto actualizado", n);
    }

    private void eliminar() {
        if (actual == null) return;
        if (!Ui.confirm(view, "¿Eliminar a " + actual.getNombre() + "?", "Se borrará su ficha y sus notas.", "Eliminar")) return;
        String n = actual.getNombre();
        if (dao.eliminar(actual.getId())) {
            fill();
            if (list.getItems().isEmpty()) limpiar(); else list.getSelectionModel().selectFirst();
            Notifier.deleted("Contacto eliminado", n);
        }
    }

    public VBox getView() { return view; }
}
