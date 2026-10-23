package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * Notificaciones dentro de la app: tarjetas pequeñas que entran deslizándose por la esquina superior derecha.
 * <ul>
 *   <li><b>Avisos importantes</b> (WARNING, ERROR): siempre se muestran y quedan en la campana.</li>
 *   <li><b>Confirmaciones</b> (SUCCESS, INFO, DELETE: "se guardó", "se eliminó"…): están APAGADAS por defecto;
 *       los recordatorios de verdad (eventos, cumpleaños) los gestiona {@code CentroAvisos}. Se pueden
 *       activar en Configuración y entonces tampoco llenan la campana.</li>
 * </ul>
 */
public final class Notifier {
    public enum Tone {
        SUCCESS("success", "toast-success"), INFO("info", "toast-info"),
        WARNING("warning", "toast-warning"), ERROR("error", "toast-error"), DELETE("delete", "toast-delete");
        final String icon, css;
        Tone(String icon, String css) { this.icon = icon; this.css = css; }
        public String icon() { return icon; }
        public String css() { return css; }
    }

    /** Elemento del historial (campana). */
    public record Entry(Tone tone, String title, String message, LocalDateTime when) {}

    private static final int MAX_VISIBLE = 4;
    private static final int MAX_HISTORY = 40;
    private static VBox layer;
    private static String ultimaHuella = "";
    private static long ultimaVez;
    private static boolean enabled = false;   // confirmaciones de cambios
    private static final ObservableList<Entry> HISTORY = FXCollections.observableArrayList();
    private static final IntegerProperty UNREAD = new SimpleIntegerProperty(0);

    private Notifier() {}

    /** Capa (sobre todo el contenido de la ventana principal) donde aparecen las tarjetas. */
    public static void install(VBox overlay) {
        layer = overlay;
        layer.setAlignment(Pos.TOP_RIGHT);
        layer.setSpacing(10);
        layer.setPadding(new Insets(72, 20, 20, 20));
        layer.setPickOnBounds(false);
        layer.setMouseTransparent(false);
    }

    /** Activa/desactiva las confirmaciones ("se guardó…"). Los avisos y errores se muestran siempre. */
    public static void setEnabled(boolean on) { enabled = on; }
    public static boolean isEnabled() { return enabled; }

    public static ObservableList<Entry> history() { return HISTORY; }
    public static IntegerProperty unreadProperty() { return UNREAD; }
    public static void markAllRead() { UNREAD.set(0); }

    public static void success(String title, String message) { show(Tone.SUCCESS, title, message); }
    public static void info(String title, String message) { show(Tone.INFO, title, message); }
    public static void warning(String title, String message) { show(Tone.WARNING, title, message); }
    public static void error(String title, String message) { show(Tone.ERROR, title, message); }
    public static void deleted(String title, String message) { show(Tone.DELETE, title, message); }

    public static void show(Tone tone, String title, String message) {
        if (!Platform.isFxApplicationThread()) { Platform.runLater(() -> show(tone, title, message)); return; }
        boolean confirmacion = tone == Tone.SUCCESS || tone == Tone.INFO || tone == Tone.DELETE;
        if (confirmacion && !enabled) return;                 // sin tarjeta, sin historial, sin contador
        // Un mismo aviso repetido en menos de 2 segundos (p. ej. por un doble evento) se muestra una sola vez.
        String huella = tone + "|" + title + "|" + message;
        long ahora = System.currentTimeMillis();
        if (huella.equals(ultimaHuella) && ahora - ultimaVez < 2000) return;
        ultimaHuella = huella;
        ultimaVez = ahora;
        HISTORY.addFirst(new Entry(tone, title, message, LocalDateTime.now()));
        if (HISTORY.size() > MAX_HISTORY) HISTORY.remove(MAX_HISTORY, HISTORY.size());
        UNREAD.set(UNREAD.get() + 1);
        if (layer == null) return;
        while (layer.getChildren().size() >= MAX_VISIBLE) layer.getChildren().removeFirst();
        HBox card = card(tone, title, message);
        layer.getChildren().add(card);
        animateIn(card);
    }

    private static HBox card(Tone tone, String title, String message) {
        StackPane badge = new StackPane(IconFactory.icon(tone.icon(), 16));
        badge.getStyleClass().addAll("toast-badge");
        Label t = new Label(title);
        t.getStyleClass().add("toast-title");
        t.setWrapText(true);
        VBox texts = new VBox(2, t);
        if (message != null && !message.isBlank()) {
            Label m = new Label(message);
            m.getStyleClass().add("toast-message");
            m.setWrapText(true);
            texts.getChildren().add(m);
        }
        HBox.setHgrow(texts, Priority.ALWAYS);
        Button close = new Button();
        close.setGraphic(IconFactory.icon("close", 14));
        close.getStyleClass().addAll("icon-button", "toast-close");
        close.setAccessibleText("Cerrar aviso");
        HBox card = new HBox(12, badge, texts, close);
        card.getStyleClass().addAll("toast", tone.css());
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(340);
        card.setMinWidth(260);
        card.setPrefWidth(320);
        card.setAlignment(Pos.CENTER_LEFT);

        PauseTransition life = new PauseTransition(Duration.seconds(tone == Tone.ERROR ? 6 : 4));
        life.setOnFinished(e -> animateOut(card));
        card.setOnMouseEntered(e -> life.pause());
        card.setOnMouseExited(e -> life.play());
        close.setOnAction(e -> { life.stop(); animateOut(card); });
        card.getProperties().put("life", life);
        life.play();
        return card;
    }

    private static void animateIn(HBox card) {
        card.setOpacity(0);
        card.setTranslateX(48);
        FadeTransition f = new FadeTransition(Duration.millis(220), card);
        f.setToValue(1);
        TranslateTransition t = new TranslateTransition(Duration.millis(320), card);
        t.setToX(0);
        t.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
        new ParallelTransition(f, t).play();
    }

    private static void animateOut(HBox card) {
        if (card.getProperties().containsKey("closing")) return;
        card.getProperties().put("closing", true);
        FadeTransition f = new FadeTransition(Duration.millis(200), card);
        f.setToValue(0);
        TranslateTransition t = new TranslateTransition(Duration.millis(200), card);
        t.setToX(40);
        ParallelTransition p = new ParallelTransition(f, t);
        p.setOnFinished(e -> { if (layer != null) layer.getChildren().remove(card); });
        p.play();
    }
}
