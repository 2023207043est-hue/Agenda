package com.agendapersonal.service;

import com.agendapersonal.controller.MainController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * Polling ligero (estilo AJAX): cada 15s recarga SQLite en segundo plano.
 * La UI se actualiza sola vía listeners de las ObservableList.
 */
public class DataRefreshService {
    private final MainController c;
    private Timeline timer;

    public DataRefreshService(MainController c) { this.c = c; }

    public void start() {
        if (timer != null) return;
        timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(15), e -> c.cargarTodoAsync()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    public void refreshNow() { c.cargarTodoAsync(); }
}
