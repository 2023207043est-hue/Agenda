package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import com.agendapersonal.service.CalendarioUnificadoService;
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
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hábitos: resumen del día, semana en puntitos, racha y check grande para marcar lo cumplido. */
public class HabitosView {
    private static final String[] DIAS = {"L", "M", "M", "J", "V", "S", "D"};
    private final HabitoDAO dao = new HabitoDAO();
    private final VBox view;
    private final HBox resumen = new HBox(0);
    private final VBox filas = new VBox(0);
    private final TextField nombre = new TextField();

    public HabitosView() {
        VBox page = Ui.page();
        Button nuevo = Ui.primary("add", "Nuevo hábito");
        nuevo.setOnAction(e -> { nombre.requestFocus(); Anim.pop(nombre); });
        page.getChildren().add(Ui.pageHeader("habits", "Hábitos",
            "Marca lo que cumpliste hoy y mantén viva tu racha.", nuevo));
        resumen.getStyleClass().addAll("card", "summary-strip");
        filas.getStyleClass().add("list-card");
        VBox lista = new VBox(0, filas, formulario());
        lista.getStyleClass().addAll("card", "card-flush");
        page.getChildren().addAll(resumen, lista);
        view = page;
        refresh();
    }

    private Node formulario() {
        nombre.setPromptText("Nuevo hábito (ej. Leer 20 minutos)");
        nombre.getStyleClass().add("bare-field");
        HBox.setHgrow(nombre, Priority.ALWAYS);
        ComboBox<String> frec = new ComboBox<>();
        frec.getItems().addAll("Todos los días", "Una vez por semana");
        frec.setValue("Todos los días");
        frec.getStyleClass().add("compact-combo");
        Button add = Ui.secondary("add", "Agregar");
        Runnable agregar = () -> {
            String n = nombre.getText().trim();
            if (n.isEmpty()) { Anim.shake(nombre); return; }
            String f = frec.getSelectionModel().getSelectedIndex() == 1 ? "SEMANAL" : "DIARIO";
            if (dao.insertar(new Habito(0, n, "", f, 1, "#2C7A4B", true))) {
                nombre.clear();
                refresh();
                Notifier.success("Hábito creado", n + " · ¡a por la primera racha!");
            } else Notifier.error("No se pudo crear el hábito", null);
        };
        add.setOnAction(e -> agregar.run());
        nombre.setOnAction(e -> agregar.run());
        HBox row = new HBox(12, IconFactory.icon("add", 18), nombre, frec, add);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("add-row");
        return row;
    }

    private void refresh() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        List<Habito> habitos = dao.listar();
        List<Habito> activos = habitos.stream().filter(Habito::isActivo).toList();
        Map<String, Set<Integer>> semana = dao.cumplidosEntre(lunes.toString(), lunes.plusDays(6).toString());
        Set<Integer> hechosHoy = semana.getOrDefault(hoy.toString(), Set.of());

        // ---- resumen
        List<Habito> deHoy = activos.stream().filter(h -> CalendarioUnificadoService.aplicaHabito(h, hoy)).toList();
        long cumplidosHoy = deHoy.stream().filter(h -> hechosHoy.contains(h.getId())).count();
        int programados = 0, cumplidos = 0;
        for (LocalDate d = lunes; !d.isAfter(hoy); d = d.plusDays(1)) {
            Set<Integer> hechos = semana.getOrDefault(d.toString(), Set.of());
            for (Habito h : activos) if (CalendarioUnificadoService.aplicaHabito(h, d)) {
                programados++;
                if (hechos.contains(h.getId())) cumplidos++;
            }
        }
        Habito mejor = null;
        int mejorRacha = 0;
        List<Integer> rachas = new ArrayList<>();
        for (Habito h : habitos) {
            int r = dao.racha(h.getId());
            rachas.add(r);
            if (h.isActivo() && r > mejorRacha) { mejorRacha = r; mejor = h; }
        }
        long faltan = deHoy.size() - cumplidosHoy;
        resumen.getChildren().setAll(
            celda("Hoy", cumplidosHoy + "", " de " + deHoy.size(),
                deHoy.isEmpty() ? "Sin hábitos para hoy." : faltan == 0 ? "¡Día completo! Excelente." : "Te faltan " + faltan + " para cerrar el día.",
                deHoy.isEmpty() ? 0 : (double) cumplidosHoy / deHoy.size()),
            celdaRacha(mejorRacha, mejor),
            celda("Esta semana", programados == 0 ? "—" : Math.round(100.0 * cumplidos / programados) + " %", "",
                cumplidos + " de " + programados + " registros cumplidos", -1));
        resumen.getChildren().getLast().getStyleClass().add("with-left-divider");

        // ---- filas
        filas.getChildren().clear();
        GridPane head = grid();
        head.add(Ui.h3("Tus hábitos"), 1, 0);
        HBox letras = new HBox(8);
        for (int i = 0; i < 7; i++) {
            Label l = Ui.label(DIAS[i], "day-letter");
            if (lunes.plusDays(i).equals(hoy)) l.getStyleClass().add("day-letter-today");
            letras.getChildren().add(l);
        }
        head.add(letras, 2, 0);
        Label rl = Ui.label("Racha", "day-letter");
        head.add(rl, 3, 0);
        GridPane.setHalignment(rl, javafx.geometry.HPos.RIGHT);
        head.getStyleClass().add("habit-head");
        filas.getChildren().add(head);

        if (habitos.isEmpty()) {
            filas.getChildren().add(Ui.empty("habits", "Crea tu primer hábito",
                "Escribe abajo algo pequeño que quieras repetir, como «Beber 2 L de agua»."));
            return;
        }
        List<Node> nodos = new ArrayList<>();
        for (int i = 0; i < habitos.size(); i++) {
            Habito h = habitos.get(i);
            Node f = fila(h, rachas.get(i), hechosHoy.contains(h.getId()), lunes, hoy, semana);
            filas.getChildren().add(f);
            nodos.add(f);
        }
        Anim.stagger(nodos);
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(20);
        ColumnConstraints c0 = new ColumnConstraints(48);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(160);
        ColumnConstraints c2 = new ColumnConstraints(7 * 28 + 6 * 8);
        ColumnConstraints c3 = new ColumnConstraints(110);
        ColumnConstraints c4 = new ColumnConstraints(40);
        g.getColumnConstraints().addAll(c0, c1, c2, c3, c4);
        g.setAlignment(Pos.CENTER_LEFT);
        return g;
    }

    private Node fila(Habito h, int racha, boolean hecho, LocalDate lunes, LocalDate hoy, Map<String, Set<Integer>> semana) {
        boolean tocaHoy = h.isActivo() && CalendarioUnificadoService.aplicaHabito(h, hoy);
        Button check = new Button();
        check.getStyleClass().addAll("habit-check");
        if (hecho) check.getStyleClass().add("checked");
        check.setGraphic(IconFactory.icon("check", 20, "tint-on-accent"));
        check.setAccessibleText((hecho ? "Desmarcar " : "Marcar ") + h.getNombre());
        Ui.tooltip(check, hecho ? "Desmarcar hoy" : "Marcar como cumplido hoy");
        check.setDisable(!h.isActivo());
        check.setOnAction(e -> {
            Anim.pop(check);
            String d = hoy.toString();
            boolean ok = hecho ? dao.desmarcar(h.getId(), d) : dao.marcar(h.getId(), d);
            if (!ok) { Notifier.error("No se pudo actualizar el hábito", null); return; }
            if (!hecho) Notifier.success("¡Bien hecho!", h.getNombre() + " · racha de " + dao.racha(h.getId()) + " días");
            else Notifier.info("Hábito desmarcado", h.getNombre());
            refresh();
        });

        Label n = new Label(h.getNombre());
        n.getStyleClass().add("row-title");
        String frec = "SEMANAL".equalsIgnoreCase(h.getFrecuencia()) ? "Una vez por semana" : "Todos los días";
        Label sub = Ui.label(h.isActivo() ? frec : "En pausa", "row-meta");
        VBox texts = new VBox(3, n, sub);

        HBox dias = new HBox(8);
        for (int i = 0; i < 7; i++) {
            LocalDate d = lunes.plusDays(i);
            boolean cumplido = semana.getOrDefault(d.toString(), Set.of()).contains(h.getId());
            StackPane dot = new StackPane();
            dot.getStyleClass().add("day-dot");
            if (cumplido) {
                dot.getStyleClass().add(d.equals(hoy) ? "day-done-today" : "day-done");
                dot.getChildren().add(IconFactory.icon("check", 13, d.equals(hoy) ? "tint-on-accent" : "tint-green"));
            } else if (d.isAfter(hoy)) dot.getStyleClass().add("day-future");
            else if (!CalendarioUnificadoService.aplicaHabito(h, d) || !h.isActivo()) {
                dot.getStyleClass().add("day-off");
                javafx.scene.layout.Region p = new javafx.scene.layout.Region();
                p.getStyleClass().add("day-off-dot");
                dot.getChildren().add(p);
            }
            else if (d.equals(hoy)) dot.getStyleClass().add("day-today");
            else dot.getStyleClass().add("day-missed");
            dias.getChildren().add(dot);
        }
        HBox rachaBox = new HBox(6);
        rachaBox.setAlignment(Pos.CENTER_RIGHT);
        if (racha > 0) {
            rachaBox.getChildren().addAll(IconFactory.icon("flame", 17, "tint-orange"),
                Ui.label(racha + (racha == 1 ? " día" : " días"), "streak"));
        } else rachaBox.getChildren().add(Ui.label(tocaHoy ? "Empieza hoy" : "—", "row-meta"));

        Button mas = Ui.iconButton("more", "Más opciones");
        mas.setOnAction(e -> menu(h).show(mas, javafx.geometry.Side.BOTTOM, 0, 4));

        GridPane g = grid();
        g.add(check, 0, 0);
        g.add(texts, 1, 0);
        g.add(dias, 2, 0);
        g.add(rachaBox, 3, 0);
        g.add(mas, 4, 0);
        g.getStyleClass().add("habit-row");
        if (!h.isActivo()) g.getStyleClass().add("row-paused");
        g.setPadding(new Insets(12, 20, 12, 20));
        return g;
    }

    private ContextMenu menu(Habito h) {
        MenuItem pausa = new MenuItem(h.isActivo() ? "Pausar hábito" : "Reanudar hábito");
        pausa.setOnAction(e -> {
            h.setActivo(!h.isActivo());
            if (dao.actualizar(h)) {
                refresh();
                Notifier.info(h.isActivo() ? "Hábito reanudado" : "Hábito en pausa", h.getNombre());
            }
        });
        MenuItem borrar = new MenuItem("Eliminar");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (Ui.confirm(view, "¿Eliminar «" + h.getNombre() + "»?", "También se borrará su historial y su racha.", "Eliminar")
                && dao.eliminar(h.getId())) {
                refresh();
                Notifier.deleted("Hábito eliminado", h.getNombre());
            }
        });
        return new ContextMenu(pausa, new SeparatorMenuItem(), borrar);
    }

    private VBox celda(String titulo, String valor, String sufijo, String nota, double progreso) {
        Label t = Ui.label(titulo, "stat-label");
        Label v = Ui.label(valor, "stat-value");
        HBox vr = new HBox(4, v);
        vr.setAlignment(Pos.BASELINE_LEFT);
        if (!sufijo.isEmpty()) vr.getChildren().add(Ui.label(sufijo, "stat-suffix"));
        VBox b = new VBox(10, t, vr);
        if (progreso >= 0) b.getChildren().add(Ui.bar(progreso, "green", 6));
        b.getChildren().add(Ui.label(nota, "stat-note"));
        b.getStyleClass().add("summary-cell");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private VBox celdaRacha(int racha, Habito mejor) {
        Label t = Ui.label("Racha más larga", "stat-label");
        Label v = Ui.label(String.valueOf(racha), "stat-value");
        HBox vr = new HBox(6, IconFactory.icon("flame", 24, "tint-orange"), v, Ui.label(racha == 1 ? " día" : " días", "stat-suffix"));
        vr.setAlignment(Pos.CENTER_LEFT);
        VBox b = new VBox(10, t, vr, Ui.label(mejor == null ? "Marca tus hábitos para empezar." : mejor.getNombre(), "stat-note"));
        b.getStyleClass().addAll("summary-cell", "with-left-divider");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private javafx.scene.control.ScrollPane scroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node getView() {
        if (scroll == null) scroll = Ui.scroll(view);
        return scroll;
    }
}
