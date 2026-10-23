package com.agendapersonal.ui;

import com.agendapersonal.ui.FontPreferences;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Calendario manipulable: navegar meses, seleccionar fecha y ver días con eventos. */
public class InteractiveCalendar {
    private final MainController c;
    private final Consumer<LocalDate> onSelect;
    private final VBox root;
    private final Label monthLabel;
    private final Label monthCount;
    private final GridPane grid;
    private YearMonth shown;
    private LocalDate selected;
    private final Locale es = Locale.of("es", "PE");

    public InteractiveCalendar(MainController c, Consumer<LocalDate> onSelect) {
        this.c = c;
        this.onSelect = onSelect;
        this.selected = LocalDate.now();
        this.shown = YearMonth.now();

        root = Ui.card();
        Label h = new Label("Calendario");
        FontPreferences.bind(h, FontWeight.BOLD, 18);

        Button prev = iconButton("arrow-left", "Mes anterior");
        Button next = iconButton("arrow-right", "Mes siguiente");
        Button todayBtn = new Button("Hoy");
        monthLabel = new Label();
        FontPreferences.bind(monthLabel, FontWeight.BOLD, 13);
        monthCount = new Label();
        monthCount.setStyle("-fx-text-fill:-muted;");
        prev.setOnAction(e -> { shown = shown.minusMonths(1); rebuild(); });
        next.setOnAction(e -> { shown = shown.plusMonths(1); rebuild(); });
        todayBtn.setOnAction(e -> {
            shown = YearMonth.now();
            selected = LocalDate.now();
            rebuild();
            onSelect.accept(selected);
        });

        HBox nav = new HBox(8, prev, monthLabel, next);
        nav.setAlignment(Pos.CENTER_LEFT);
        HBox top = new HBox(8, nav, spacer(), todayBtn);
        top.setAlignment(Pos.CENTER_LEFT);

        grid = new GridPane();
        grid.setHgap(7);
        grid.setVgap(7);

        HBox legend = new HBox(12,
            legendItem("-accent", "Hoy"),
            legendItem("-accent-soft", "Con eventos"),
            legendItem("-text", "Seleccionado"));
        legend.setPadding(new Insets(4, 0, 0, 0));

        root.getChildren().addAll(h, top, monthCount, grid, legend);
        rebuild();
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Button iconButton(String icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(IconFactory.image(icon, 16));
        button.getStyleClass().add("icon-button");
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        return button;
    }

    private HBox legendItem(String color, String text) {
        Label dot = new Label("  ");
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;-fx-min-width:14;-fx-min-height:14;");
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12;-fx-text-fill:-muted;");
        HBox b = new HBox(5, dot, l);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    public VBox getView() { return root; }
    public LocalDate getSelected() { return selected; }

    public void setSelected(LocalDate d) {
        selected = d;
        if (d != null) shown = YearMonth.from(d);
        rebuild();
    }

    /** Solo repinta marcas de eventos (llamado por el auto-refresh). */
    public void refreshMarks() { rebuild(); }

    private void rebuild() {
        monthLabel.setText(cap(shown.getMonth().getDisplayName(TextStyle.FULL, es)) + " " + shown.getYear());
        grid.getChildren().clear();
        String[] ds = {"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label z = new Label(ds[i]);
            z.setPrefWidth(45);
            z.setAlignment(Pos.CENTER);
            z.setStyle("-fx-text-fill:-muted;");
            grid.add(z, i, 0);
        }
        // Conteo de eventos del mes (una sola pasada)
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (var e : c.getEventos()) {
            if (e.getFecha() != null && YearMonth.from(e.getFecha()).equals(shown))
                porDia.merge(e.getFecha(), 1L, Long::sum);
        }
        monthCount.setText(porDia.isEmpty() ? "Sin eventos este mes"
            : porDia.values().stream().mapToLong(Long::longValue).sum() + " evento(s) este mes");

        LocalDate today = LocalDate.now();
        int off = shown.atDay(1).getDayOfWeek().getValue() - 1;
        for (int day = 1; day <= shown.lengthOfMonth(); day++) {
            LocalDate fecha = shown.atDay(day);
            long n = porDia.getOrDefault(fecha, 0L);
            Button b = new Button(n > 0 ? day + " •" : String.valueOf(day));
            b.setPrefSize(45, 38);
            b.getStyleClass().add("day-btn");
            b.setAlignment(Pos.CENTER);
            b.setStyle(baseStyle(fecha, today, n > 0));
            if (n > 0) b.setTooltip(new Tooltip(n + " evento(s) el " + day + "/" + shown.getMonthValue()));
            b.setOnAction(e -> {
                selected = fecha;
                rebuild();
                onSelect.accept(fecha);
            });
            int k = off + day - 1;
            grid.add(b, k % 7, k / 7 + 1);
        }
    }

    private String baseStyle(LocalDate fecha, LocalDate today, boolean hasEvents) {
        if (fecha.equals(selected))
            return "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        if (fecha.equals(today))
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-background-radius:20;-fx-cursor:hand;";
        if (hasEvents)
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        return "-fx-background-color:transparent;-fx-text-fill:-text;-fx-cursor:hand;";
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(es) + s.substring(1);
    }
}

