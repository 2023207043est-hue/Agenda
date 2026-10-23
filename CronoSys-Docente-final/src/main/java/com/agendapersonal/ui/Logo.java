package com.agendapersonal.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Logo de Compás: una brújula que también es un reloj, con la aguja saliendo por la abertura del anillo.
 * Se dibuja en vector, así que se ve nítido en cualquier tamaño; los PNG y el .ico del programa se
 * generan con el mismo diseño (carpeta /brand).
 */
public final class Logo {
    public static final Color NARANJA_CLARO = Color.web("#F2884F");
    public static final Color NARANJA_OSCURO = Color.web("#C2451A");
    public static final Color AGUJA = Color.web("#FFD9C2");
    /** Nombre de la aplicación. */
    public static final String NOMBRE = "Compás";
    private static final int[] TAMANOS_ICONO = {16, 24, 32, 48, 64, 128, 256};

    private Logo() {}

    /** Marca cuadrada (azulejo naranja con la brújula en blanco). */
    public static Node mark(double s) {
        Rectangle tile = new Rectangle(s, s);
        tile.setArcWidth(s * 0.56);
        tile.setArcHeight(s * 0.56);
        tile.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, NARANJA_CLARO), new Stop(1, NARANJA_OSCURO)));

        // El diseño está en una rejilla de 256×256 (igual que el SVG y los PNG).
        double u = s / 256.0;
        double cx = s / 2, cy = s / 2;

        // El anillo: un arco con la abertura arriba. En JavaFX el constructor es
        // (rx, ry, rotación, x, y, arcoGrande, sentido) y las banderas van al final.
        Path anillo = new Path(
            new MoveTo(83.28 * u, 64.13 * u),
            new ArcTo(78 * u, 78 * u, 0, 172.72 * u, 64.13 * u, true, false));
        anillo.setFill(null);
        anillo.setStroke(Color.WHITE);
        anillo.setStrokeWidth(26 * u);
        anillo.setStrokeLineCap(StrokeLineCap.ROUND);

        double punta = cy - 70 * u, medio = 16 * u;
        Polygon izquierda = new Polygon(cx, punta, cx - medio, cy, cx, cy);
        izquierda.setFill(AGUJA);
        Polygon derecha = new Polygon(cx, punta, cx + medio, cy, cx, cy);
        derecha.setFill(Color.WHITE);

        double cr = 12 * u;
        Circle centro = new Circle(cx, cy, cr, Color.WHITE);

        Group g = new Group(tile, anillo, izquierda, derecha, centro);
        g.setMouseTransparent(true);
        g.setAccessibleText(NOMBRE);
        return g;
    }

    /** Marca + nombre ("Comp" en texto normal y "ás" en el color de acento). */
    public static HBox wordmark(double s) {
        Label comp = new Label("Comp");
        comp.getStyleClass().add("brand-name");
        Label as = new Label("ás");
        as.getStyleClass().addAll("brand-name", "brand-name-accent");
        HBox name = new HBox(0, comp, as);
        name.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(10, mark(s), name);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Íconos de la ventana (barra de tareas, Alt+Tab) desde /brand. */
    public static List<Image> iconosVentana() {
        List<Image> r = new ArrayList<>();
        for (int t : TAMANOS_ICONO) {
            var url = Logo.class.getResource("/brand/compas-" + t + ".png");
            if (url != null) r.add(new Image(url.toExternalForm()));
        }
        return r;
    }

    public static void aplicarIconos(Stage stage) {
        if (stage != null && stage.getIcons().isEmpty()) stage.getIcons().addAll(iconosVentana());
    }
}
