package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.List;

/** Pequeñas animaciones reutilizables (entrada de páginas, "pop" al marcar, sacudida por error). */
public final class Anim {
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.2, 0.9, 0.2, 1);

    private Anim() {}

    /** Entrada suave de una página: aparece y sube unos píxeles. */
    public static void pageIn(Node n) {
        if (ThemeService.get().reduceMotion()) { n.setOpacity(1); n.setTranslateY(0); return; }
        n.setOpacity(0);
        n.setTranslateY(10);
        FadeTransition f = new FadeTransition(Duration.millis(200), n);
        f.setToValue(1);
        TranslateTransition t = new TranslateTransition(Duration.millis(280), n);
        t.setToY(0);
        t.setInterpolator(EASE_OUT);
        new ParallelTransition(f, t).play();
    }

    /** Efecto de "pulsación" al marcar algo (check de tarea o hábito, favorito...). */
    public static void pop(Node n) {
        if (ThemeService.get().reduceMotion()) return;
        ScaleTransition down = new ScaleTransition(Duration.millis(90), n);
        down.setToX(0.82);
        down.setToY(0.82);
        ScaleTransition up = new ScaleTransition(Duration.millis(220), n);
        up.setToX(1);
        up.setToY(1);
        up.setInterpolator(Interpolator.SPLINE(0.3, 1.6, 0.5, 1));
        new SequentialTransition(down, up).play();
    }

    /** Sacude un campo para indicar que falta un dato. */
    public static void shake(Node n) {
        if (ThemeService.get().reduceMotion()) { n.requestFocus(); return; }
        TranslateTransition t = new TranslateTransition(Duration.millis(60), n);
        t.setFromX(0);
        t.setByX(7);
        t.setCycleCount(6);
        t.setAutoReverse(true);
        t.setOnFinished(e -> n.setTranslateX(0));
        t.play();
        n.requestFocus();
    }

    /** Hace aparecer una lista de elementos uno detrás de otro. */
    public static void stagger(List<? extends Node> nodes) {
        if (ThemeService.get().reduceMotion()) { nodes.forEach(x -> { x.setOpacity(1); x.setTranslateY(0); }); return; }
        int i = 0;
        for (Node n : nodes) {
            if (i > 12) break;                       // con muchos elementos no hace falta animar todo
            n.setOpacity(0);
            n.setTranslateY(6);
            FadeTransition f = new FadeTransition(Duration.millis(180), n);
            f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(240), n);
            t.setToY(0);
            t.setInterpolator(EASE_OUT);
            ParallelTransition p = new ParallelTransition(f, t);
            p.setDelay(Duration.millis(28L * i++));
            p.play();
        }
    }

    /** Aparición simple. */
    public static void fadeIn(Node n, double ms) {
        if (ThemeService.get().reduceMotion()) { n.setOpacity(1); return; }
        n.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(ms), n);
        f.setToValue(1);
        f.play();
    }
}
