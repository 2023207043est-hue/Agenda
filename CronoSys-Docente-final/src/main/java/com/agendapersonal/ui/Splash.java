package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Pantalla de carga al abrir y despedida al salir. Se dibuja antes de que exista la base de datos
 * y el tema, así que usa los colores de marca fijos (por eso aquí sí hay colores escritos a mano).
 */
public final class Splash {
    private static final String TARJETA = "-fx-background-color: #FBFAF7; -fx-background-radius: 22; -fx-border-radius: 22; -fx-border-color: #E7E5DF;";
    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final Label estado = new Label();
    private final ProgressBar barra = new ProgressBar(0);
    private final StackPane raiz = new StackPane();

    private Splash(String titulo, String subtitulo, boolean conBarra) {
        Label nombre = new Label(titulo);
        nombre.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1C1F26;");
        Label sub = new Label(subtitulo);
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #646978;");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        estado.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8F9C;");
        barra.setPrefWidth(240);
        barra.setPrefHeight(6);
        barra.setStyle("-fx-accent: #E06A33; -fx-control-inner-background: #ECE9E2; -fx-background-color: transparent;");
        VBox caja = new VBox(14, Logo.mark(84), nombre, sub);
        caja.setAlignment(Pos.CENTER);
        if (conBarra) caja.getChildren().addAll(barra, estado);
        caja.setPadding(new Insets(34, 40, 30, 40));
        caja.setStyle(TARJETA);
        caja.setPrefSize(400, conBarra ? 330 : 280);
        caja.setMaxSize(400, conBarra ? 330 : 280);
        caja.setEffect(new DropShadow(28, 0, 8, Color.rgb(28, 31, 38, 0.25)));
        raiz.getChildren().add(caja);
        raiz.setPadding(new Insets(34));
        raiz.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(raiz, 468, conBarra ? 398 : 348);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Compás");
        stage.setAlwaysOnTop(true);
        var vb = Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMinX() + (vb.getWidth() - 468) / 2);
        stage.setY(vb.getMinY() + (vb.getHeight() - scene.getHeight()) / 2);
        try { Logo.aplicarIconos(stage); } catch (RuntimeException ignored) { }
        raiz.setOpacity(0);
    }

    /** Carga al abrir. Llama a {@link #avance} entre pasos y a {@link #cerrar} al terminar. */
    public static Splash carga() { return new Splash("Compás", "Tu tiempo, mejor invertido", true); }

    public void mostrar() {
        stage.show();
        FadeTransition f = new FadeTransition(Duration.millis(260), raiz);
        f.setToValue(1);
        f.play();
    }

    /** Actualiza el mensaje y la barra (0 a 1). */
    public void avance(String mensaje, double progreso) {
        estado.setText(mensaje);
        barra.setProgress(Math.max(0, Math.min(1, progreso)));
    }

    /** Se desvanece y cierra. {@code alCerrar} se ejecuta al terminar. */
    public void cerrar(Runnable alCerrar) {
        FadeTransition f = new FadeTransition(Duration.millis(320), raiz);
        f.setToValue(0);
        f.setOnFinished(e -> { stage.close(); if (alCerrar != null) alCerrar.run(); });
        f.play();
    }

    /** Despedida: "Hasta pronto" un instante y luego {@code alTerminar} (la salida real). */
    public static void despedida(String nombre, Runnable alTerminar) {
        String quien = nombre == null || nombre.isBlank() ? "" : ", " + nombre.trim();
        Splash s = new Splash("Hasta pronto" + quien, "Tus datos ya están guardados en este equipo.", false);
        s.mostrar();
        PauseTransition espera = new PauseTransition(Duration.millis(950));
        espera.setOnFinished(e -> s.cerrar(alTerminar));
        espera.play();
    }
}
