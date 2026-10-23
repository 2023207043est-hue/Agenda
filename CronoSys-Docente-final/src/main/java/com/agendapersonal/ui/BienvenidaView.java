package com.agendapersonal.ui;

import com.agendapersonal.util.Perfil;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Pantalla de bienvenida de la primera vez: pide el nombre (sin contraseña) y lo usa en el saludo. */
public final class BienvenidaView {
    private BienvenidaView() {}

    /** Muestra la bienvenida y espera a que se cierre. Solo aparece la primera vez. */
    public static void mostrarSiHaceFalta() {
        if (Perfil.bienvenidaVista()) return;
        Stage st = new Stage();
        st.setTitle("Bienvenido a Compás");
        Logo.aplicarIconos(st);
        st.setResizable(false);

        Label hola = new Label("¡Te damos la bienvenida!");
        hola.getStyleClass().add("h1");
        Label sub = new Label("Personaliza tu espacio de trabajo. Todo queda guardado en este equipo.");
        sub.setWrapText(true);
        sub.setStyle("-fx-text-fill:-muted;-fx-text-alignment:center;");
        sub.setAlignment(Pos.CENTER);

        TextField nombre = new TextField();
        nombre.setPromptText("Tu nombre");
        nombre.setMaxWidth(320);

        Label nombreLabel = new Label("¿Cómo te llamas?");
        nombreLabel.getStyleClass().add("h3");
        nombre.setAccessibleText("Tu nombre");

        ToggleGroup grupoRol = new ToggleGroup();
        ToggleButton estudiante = opcionRol(grupoRol, "book", "Estudiante",
            "Organiza tus cursos, estudio y tareas.");
        ToggleButton docente = opcionRol(grupoRol, "calendar", "Docente",
            "Prepara clases, evaluaciones y tutorías.");
        estudiante.setSelected(true);
        HBox opciones = new HBox(12, estudiante, docente);
        opciones.setMaxWidth(440);
        opciones.setAlignment(Pos.CENTER);

        Button empezar = Ui.primary(null, "Empezar");
        empezar.setDefaultButton(true);
        empezar.disableProperty().bind(nombre.textProperty().map(t -> Perfil.limpiar(t).isEmpty()));
        empezar.setOnAction(e -> {
            Perfil.guardarNombre(nombre.getText());
            Perfil.guardarRol(docente.isSelected() ? Perfil.DOCENTE : Perfil.ESTUDIANTE);
            st.close();
        });
        Button omitir = Ui.ghost(null, "Omitir por ahora");
        omitir.setOnAction(e -> { guardarRol(docente.isSelected()); Perfil.omitirBienvenida(); st.close(); });
        Label nota = new Label("Puedes cambiar el perfil después en Configuración → Tu perfil.");
        nota.getStyleClass().add("caption");

        VBox raiz = new VBox(14, Logo.mark(64), hola, sub, nombreLabel, nombre, opciones, empezar, omitir, nota);
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new Insets(28, 40, 24, 40));
        raiz.setStyle("-fx-background-color:-bg;");
        Scene sc = new Scene(raiz, 570, 600);
        ThemeService.get().applyTo(sc);
        st.setScene(sc);
        st.setOnCloseRequest(e -> { guardarRol(docente.isSelected()); Perfil.omitirBienvenida(); });
        st.showAndWait();
    }

    private static ToggleButton opcionRol(ToggleGroup grupo, String icono, String nombre, String descripcion) {
        Label titulo = new Label(nombre);
        titulo.getStyleClass().add("h3");
        Label detalle = new Label(descripcion);
        detalle.getStyleClass().add("caption");
        detalle.setWrapText(true);
        VBox contenido = new VBox(7, IconFactory.icon(icono, 22), titulo, detalle);
        contenido.setAlignment(Pos.CENTER_LEFT);
        ToggleButton boton = new ToggleButton();
        boton.setToggleGroup(grupo);
        boton.setGraphic(contenido);
        boton.setAccessibleText(nombre + ". " + descripcion);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setMinHeight(128);
        boton.setPrefHeight(128);
        boton.getStyleClass().add("role-choice");
        HBox.setHgrow(boton, javafx.scene.layout.Priority.ALWAYS);
        return boton;
    }

    private static void guardarRol(boolean docente) {
        Perfil.guardarRol(docente ? Perfil.DOCENTE : Perfil.ESTUDIANTE);
    }
}
