package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.Contacto;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.infrastructure.repo.ContactoDAO;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import java.time.LocalDate;

/** Diálogos rápidos del menú "Crear" para lo que no tiene formulario propio (hábitos y cumpleaños). */
public final class DialogosAgenda {
    private DialogosAgenda() {}

    public static void nuevoHabito(Window dueno, Runnable alGuardar) {
        Dialog<ButtonType> d = base(dueno, "Nuevo hábito",
            "Aparecerá cada día en tu calendario para que lo marques como cumplido.");
        TextField nombre = new TextField();
        nombre.setPromptText("Ej. Leer 20 min");
        nombre.setPrefWidth(260);
        ComboBox<String> frecuencia = new ComboBox<>();
        frecuencia.getItems().addAll("DIARIO", "SEMANAL");
        frecuencia.setValue("DIARIO");
        GridPane g = rejilla();
        g.addRow(0, new Label("Nombre"), nombre);
        g.addRow(1, new Label("Frecuencia"), frecuencia);
        d.getDialogPane().setContent(g);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(Bindings.createBooleanBinding(() -> nombre.getText().isBlank(), nombre.textProperty()));
        d.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            boolean guardado = new HabitoDAO().insertar(
                new Habito(0, nombre.getText().trim(), "", frecuencia.getValue(), 1, "#2C7A4B", true));
            if (guardado) { alGuardar.run(); com.agendapersonal.ui.Notifier.success("Hábito creado", nombre.getText().trim()); }
            else com.agendapersonal.ui.Notifier.error("No se pudo guardar el hábito", null);
        });
    }

    /** Crea un contacto (visible también en CRM Personal) con su fecha de cumpleaños. */
    public static void nuevoCumple(Window dueno, Runnable alGuardar) {
        Dialog<ButtonType> d = base(dueno, "Nuevo cumpleaños",
            "Se repetirá cada año en tu calendario y te avisará al iniciar Compás.");
        TextField nombre = new TextField();
        nombre.setPromptText("Nombre de la persona");
        nombre.setPrefWidth(260);
        DatePicker fecha = new DatePicker();
        fecha.setPromptText("Fecha de nacimiento");
        fecha.setEditable(false);
        fecha.setValue(LocalDate.now().minusYears(25));
        GridPane g = rejilla();
        g.addRow(0, new Label("Nombre"), nombre);
        g.addRow(1, new Label("Nacimiento"), fecha);
        d.getDialogPane().setContent(g);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(Bindings.createBooleanBinding(
            () -> nombre.getText().isBlank() || fecha.getValue() == null, nombre.textProperty(), fecha.valueProperty()));
        d.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            boolean guardado = new ContactoDAO().insertar(new Contacto(0, nombre.getText().trim(), "", "",
                "Personal", fecha.getValue().toString(), "", false, null));
            if (guardado) { alGuardar.run(); com.agendapersonal.ui.Notifier.success("Cumpleaños agregado", nombre.getText().trim() + " · " + com.agendapersonal.util.Ui.dayMonth(fecha.getValue())); }
            else com.agendapersonal.ui.Notifier.error("No se pudo guardar el cumpleaños", null);
        });
    }

    private static Dialog<ButtonType> base(Window dueno, String titulo, String cabecera) {
        Dialog<ButtonType> d = new Dialog<>();
        if (dueno != null) d.initOwner(dueno);
        d.setTitle(titulo);
        d.setHeaderText(cabecera);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().getStyleClass().add("cs-dialog");
        ((Button) d.getDialogPane().lookupButton(ButtonType.OK)).setText("Guardar");
        ((Button) d.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
        return d;
    }

    private static GridPane rejilla() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(12);
        g.setPadding(new Insets(14));
        return g;
    }
}
