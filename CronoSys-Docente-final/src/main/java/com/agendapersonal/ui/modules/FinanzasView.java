package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.Movimiento;
import com.agendapersonal.infrastructure.repo.MovimientoDAO;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finanzas: ingresos y gastos del mes, balance automático y en qué se va el dinero. */
public class FinanzasView {
    private static final List<String> GASTOS = List.of("Comida", "Transporte", "Vivienda", "Servicios", "Salud", "Ocio", "Compras", "Otros");
    private static final List<String> INGRESOS = List.of("Sueldo", "Ventas", "Ingreso extra", "Otros");
    private static final Map<String, String[]> CAT = Map.ofEntries(  // ícono, color
        Map.entry("Comida", new String[]{"food", "amber"}), Map.entry("Transporte", new String[]{"transport", "blue"}),
        Map.entry("Vivienda", new String[]{"home", "rose"}), Map.entry("Servicios", new String[]{"bolt", "teal"}),
        Map.entry("Salud", new String[]{"medical", "green"}), Map.entry("Ocio", new String[]{"leisure", "violet"}),
        Map.entry("Compras", new String[]{"shopping", "orange"}), Map.entry("Sueldo", new String[]{"work", "green"}),
        Map.entry("Ventas", new String[]{"coins", "green"}), Map.entry("Ingreso extra", new String[]{"sparkle", "green"}),
        Map.entry("Otros", new String[]{"tag", "slate"}));

    private final MovimientoDAO dao = new MovimientoDAO();
    private final VBox view;
    private YearMonth mes = YearMonth.now();
    private final Label mesLabel = Ui.label("", "month-label");
    private final HBox stats = new HBox(16);
    private final VBox movimientos = new VBox(0);
    private final VBox categorias = new VBox(14);
    private final Ui.Segmented tipo = new Ui.Segmented("Gasto", "Ingreso");
    private final ComboBox<String> cat = new ComboBox<>();
    private final TextField monto = new TextField();
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TextField desc = new TextField();

    public FinanzasView() {
        VBox page = Ui.page();
        page.getChildren().add(Ui.pageHeader("finance", "Finanzas",
            "Registra ingresos y gastos; el balance del mes se calcula solo.", selectorMes()));
        HBox listas = new HBox(24, panelMovimientos(), panelCategorias());
        page.getChildren().addAll(stats, formulario(), listas);
        view = page;
        fill();
    }

    private Node selectorMes() {
        Button prev = Ui.iconButton("chevron-left", "Mes anterior");
        Button next = Ui.iconButton("chevron-right", "Mes siguiente");
        prev.setOnAction(e -> { mes = mes.minusMonths(1); fill(); });
        next.setOnAction(e -> { mes = mes.plusMonths(1); fill(); });
        mesLabel.setMinWidth(130);
        mesLabel.setAlignment(Pos.CENTER);
        HBox box = new HBox(2, prev, mesLabel, next);
        box.getStyleClass().add("month-switch");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Node formulario() {
        cat.getItems().setAll(GASTOS);
        cat.setValue("Comida");
        tipo.selectedProperty().addListener((o, a, b) -> {
            cat.getItems().setAll(b.intValue() == 1 ? INGRESOS : GASTOS);
            cat.getSelectionModel().selectFirst();
            tipo.node().getStyleClass().removeAll("seg-income", "seg-expense");
            tipo.node().getStyleClass().add(b.intValue() == 1 ? "seg-income" : "seg-expense");
        });
        tipo.node().getStyleClass().add("seg-expense");
        monto.setPromptText("0,00");
        monto.getStyleClass().add("bare-field");
        Label prefijo = Ui.label("S/", "input-prefix");
        prefijo.setMinWidth(Region.USE_PREF_SIZE);
        HBox montoBox = new HBox(8, prefijo, monto);
        Ui.fechaCorta(fecha);
        montoBox.getStyleClass().add("input-with-icon");
        montoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(monto, Priority.ALWAYS);
        desc.setPromptText("Ej. Almuerzo con el equipo");
        Button add = Ui.primary("add", "Registrar");
        add.setOnAction(e -> registrar(montoBox));
        monto.setOnAction(e -> registrar(montoBox));
        desc.setOnAction(e -> registrar(montoBox));

        VBox fTipo = Ui.field("Tipo", tipo.node());
        HBox.setHgrow(fTipo, Priority.NEVER);
        VBox fCat = Ui.field("Categoría", cat);
        VBox fMonto = Ui.field("Monto", montoBox);
        VBox fFecha = Ui.field("Fecha", fecha);
        VBox fDesc = Ui.field("Descripción", desc);
        fCat.setPrefWidth(170); fMonto.setPrefWidth(150); fFecha.setPrefWidth(170); fDesc.setPrefWidth(260);
        HBox fila = new HBox(12, fTipo, fCat, fMonto, fFecha, fDesc, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox card = Ui.card();
        card.getChildren().addAll(Ui.h3("Nuevo movimiento"), fila);
        return card;
    }

    private void registrar(Node montoBox) {
        double m;
        try { m = Ui.parseNumber(monto.getText()); }
        catch (Exception x) { Anim.shake(montoBox); Notifier.warning("Monto inválido", "Escribe un número, por ejemplo 25,50."); return; }
        if (m <= 0) { Anim.shake(montoBox); Notifier.warning("Monto inválido", "El monto debe ser mayor que cero."); return; }
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        String t = tipo.selected() == 1 ? "INGRESO" : "GASTO";
        if (dao.insertar(new Movimiento(0, t, cat.getValue(), m, fecha.getValue().toString(), desc.getText().trim()))) {
            Notifier.success(t.equals("INGRESO") ? "Ingreso registrado" : "Gasto registrado",
                cat.getValue() + " · " + Ui.money(m));
            monto.clear();
            desc.clear();
            mes = YearMonth.from(fecha.getValue());
            fill();
            monto.requestFocus();
        } else Notifier.error("No se pudo registrar el movimiento", null);
    }

    private Node panelMovimientos() {
        VBox card = new VBox(0, movimientos);
        card.getStyleClass().addAll("card", "card-flush");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Node panelCategorias() {
        VBox card = Ui.card();
        card.setPrefWidth(330);
        card.setMinWidth(280);
        card.setMaxWidth(360);
        card.getChildren().add(categorias);
        return card;
    }

    private void fill() {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, Ui.ES);
        mesLabel.setText(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " " + mes.getYear());
        List<Movimiento> lista = dao.listarMes(mes.toString());
        double ing = 0, gas = 0;
        int nIng = 0, nGas = 0;
        Map<String, Double> porCat = new LinkedHashMap<>();
        for (Movimiento m : lista) {
            if ("INGRESO".equals(m.getTipo())) { ing += m.getMonto(); nIng++; }
            else { gas += m.getMonto(); nGas++; porCat.merge(m.getCategoria(), m.getMonto(), Double::sum); }
        }
        double neto = ing - gas;
        VBox balance = Ui.stat("Balance del mes", Ui.money(neto),
            ing > 0 ? (neto >= 0 ? "Ahorras el " + Math.round(100 * neto / ing) + " % de lo que ingresas" : "Gastas más de lo que ingresas")
                : "Registra tus ingresos para ver tu ahorro", "finance", "inverse");
        balance.getStyleClass().add("stat-inverse");
        stats.getChildren().setAll(
            Ui.stat("Ingresos", Ui.money(ing), nIng + (nIng == 1 ? " movimiento" : " movimientos"), "arrow-down-left", "green"),
            Ui.stat("Gastos", Ui.money(gas), nGas + (nGas == 1 ? " movimiento" : " movimientos"), "arrow-up-right", "rose"),
            balance);

        // ---- lista de movimientos
        movimientos.getChildren().clear();
        HBox head = new HBox(8, Ui.h3("Movimientos de " + nombreMes), Ui.spacer(),
            Ui.label(lista.size() + (lista.size() == 1 ? " movimiento" : " movimientos"), "caption"));
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(18, 22, 10, 22));
        movimientos.getChildren().add(head);
        if (lista.isEmpty()) {
            movimientos.getChildren().add(Ui.empty("finance", "Sin movimientos este mes",
                "Registra tu primer gasto o ingreso con el formulario de arriba."));
        } else {
            List<Node> filas = new ArrayList<>();
            for (Movimiento m : lista) { Node f = fila(m); movimientos.getChildren().add(f); filas.add(f); }
            Anim.stagger(filas);
        }

        // ---- gastos por categoría
        categorias.getChildren().setAll(Ui.h3("Gastos por categoría"));
        if (porCat.isEmpty()) {
            categorias.getChildren().add(Ui.muted("Aún no hay gastos en " + nombreMes + "."));
            return;
        }
        List<Map.Entry<String, Double>> orden = porCat.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).toList();
        HBox stacked = new HBox(2);
        stacked.getStyleClass().add("stacked-bar");
        for (var en : orden) {
            Region seg = new Region();
            seg.getStyleClass().addAll("stacked-seg", "fill-" + color(en.getKey()));
            seg.prefWidthProperty().bind(stacked.widthProperty().multiply(en.getValue() / gas));
            seg.setMinWidth(3);
            stacked.getChildren().add(seg);
        }
        categorias.getChildren().add(stacked);
        for (var en : orden) {
            Region sq = new Region();
            sq.getStyleClass().addAll("legend-dot", "fill-" + color(en.getKey()));
            Label n = new Label(en.getKey());
            n.getStyleClass().add("legend-name");
            HBox.setHgrow(n, Priority.ALWAYS);
            n.setMaxWidth(Double.MAX_VALUE);
            Label pct = Ui.label(Math.round(100 * en.getValue() / gas) + " %", "caption");
            Label val = Ui.label(Ui.money(en.getValue()), "legend-value");
            val.setMinWidth(96);
            val.setAlignment(Pos.CENTER_RIGHT);
            HBox r = new HBox(10, sq, n, pct, val);
            r.setAlignment(Pos.CENTER_LEFT);
            categorias.getChildren().add(r);
        }
        String top = orden.getFirst().getKey();
        HBox tip = new HBox(10, IconFactory.icon("idea", 16, "tint-teal"),
            Ui.label(top + " es tu mayor gasto de " + nombreMes + " (" + Math.round(100 * orden.getFirst().getValue() / gas) + " %).", "tip-text"));
        tip.getStyleClass().add("tip");
        ((Label) tip.getChildren().get(1)).setWrapText(true);
        categorias.getChildren().add(tip);
    }

    private static String color(String c) { return CAT.getOrDefault(c, new String[]{"tag", "slate"})[1]; }
    private static String icono(String c) { return CAT.getOrDefault(c, new String[]{"tag", "slate"})[0]; }

    private Node fila(Movimiento m) {
        boolean ingreso = "INGRESO".equals(m.getTipo());
        StackPane chip = Ui.iconChip(icono(m.getCategoria()), color(m.getCategoria()), 40, 18);
        String titulo = m.getDescripcion() == null || m.getDescripcion().isBlank() ? m.getCategoria() : m.getDescripcion();
        Label t = Ui.label(titulo, "row-title");
        String cuando;
        try { cuando = Ui.friendlyDate(LocalDate.parse(m.getFecha())); } catch (Exception e) { cuando = m.getFecha(); }
        Label sub = Ui.label(m.getCategoria() + " · " + cuando, "row-meta");
        VBox texts = new VBox(3, t, sub);
        HBox.setHgrow(texts, Priority.ALWAYS);
        Label monto = Ui.label((ingreso ? "+ " : "− ") + Ui.money(m.getMonto()), "amount");
        if (ingreso) monto.getStyleClass().add("amount-income");
        Button mas = Ui.iconButton("more", "Más opciones");
        MenuItem borrar = new MenuItem("Eliminar movimiento");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (Ui.confirm(view, "¿Eliminar este movimiento?", titulo + " · " + Ui.money(m.getMonto()), "Eliminar")
                && dao.eliminar(m.getId())) {
                fill();
                Notifier.deleted("Movimiento eliminado", titulo + " · " + Ui.money(m.getMonto()));
            }
        });
        ContextMenu cm = new ContextMenu(borrar);
        mas.setOnAction(e -> cm.show(mas, javafx.geometry.Side.BOTTOM, 0, 4));
        HBox row = new HBox(14, chip, texts, monto, mas);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("list-row", "with-divider");
        return row;
    }

    private javafx.scene.control.ScrollPane scroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node getView() {
        if (scroll == null) scroll = Ui.scroll(view);
        return scroll;
    }
}
