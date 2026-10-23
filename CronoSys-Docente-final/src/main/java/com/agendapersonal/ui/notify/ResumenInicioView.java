package com.agendapersonal.ui.notify;

import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.ContactoService;
import com.agendapersonal.service.RecordatorioService.Resumen;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.Logo;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ventana con el resumen del día (cumpleaños, eventos, tareas, hábitos). Usa los colores del tema
 * activo: antes tenía colores oscuros fijos y no cambiaba con la apariencia.
 */
public final class ResumenInicioView {
    private ResumenInicioView() {}

    public static void mostrar(Window dueno, Resumen r, Runnable irAgenda, Consumer<Boolean> alCambiarNoMostrar, boolean alIniciar) {
        Stage d = new Stage();
        if (dueno != null) d.initOwner(dueno);
        d.setTitle("Compás — Resumen de hoy");
        Logo.aplicarIconos(d);

        Label saludo = Ui.label(Ui.formatDateShort(r.hoy()), "h2");
        Label sub = Ui.muted("Esto es lo que tienes para hoy");
        HBox head = new HBox(14, Logo.mark(40), new VBox(2, saludo, sub));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox cuerpo = new VBox(18, head);
        cuerpo.setPadding(new Insets(22));
        cuerpo.getStyleClass().add("page");

        if (!r.cumples().isEmpty()) {
            VBox s = seccion("Cumpleaños", "birthday", "rose");
            for (ContactoService.Cumple c : r.cumples()) {
                String cuando = c.enDias() == 0 ? "¡Hoy!" : c.enDias() == 1 ? "Mañana"
                    : "En " + c.enDias() + " días (" + c.fecha().format(DateTimeFormatter.ofPattern("d/MM")) + ")";
                String edad = c.edad() > 0 ? " · cumple " + c.edad() : "";
                s.getChildren().add(fila(cuando, c.contacto().getNombre() + edad, "rose", c.enDias() == 0));
            }
            cuerpo.getChildren().add(s);
        }
        if (!r.eventosHoy().isEmpty()) {
            VBox s = seccion("Eventos de hoy", "event", "teal");
            for (Evento e : r.eventosHoy()) s.getChildren().add(filaEvento(e));
            cuerpo.getChildren().add(s);
        }
        if (!r.eventosManana().isEmpty()) {
            VBox s = seccion("Eventos de mañana", "calendar", "blue");
            for (Evento e : r.eventosManana()) s.getChildren().add(filaEvento(e));
            cuerpo.getChildren().add(s);
        }
        if (!r.tareasVencidas().isEmpty() || !r.tareasHoy().isEmpty()) {
            VBox s = seccion("Tareas pendientes", "tasks", "violet");
            for (Tarea t : r.tareasVencidas())
                s.getChildren().add(fila("Vencida " + t.getFecha().format(DateTimeFormatter.ofPattern("d/MM")), t.getTitulo(), "rose", false));
            for (Tarea t : r.tareasHoy()) s.getChildren().add(fila("Para hoy", t.getTitulo(), "violet", false));
            cuerpo.getChildren().add(s);
        }
        List<Habito> hab = r.habitosPendientes();
        if (!hab.isEmpty()) {
            VBox s = seccion("Hábitos de hoy sin marcar", "habits", "green");
            int n = 0;
            for (Habito h : hab) {
                if (n++ == 5) { s.getChildren().add(fila("", "… y " + (hab.size() - 5) + " más", "green", false)); break; }
                s.getChildren().add(fila("Pendiente", h.getNombre(), "green", false));
            }
            cuerpo.getChildren().add(s);
        }
        if (cuerpo.getChildren().size() == 1)
            cuerpo.getChildren().add(Ui.empty("success", "Día despejado", "No hay pendientes para hoy. ¡Disfrútalo!"));

        ScrollPane sp = new ScrollPane(cuerpo);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        CheckBox mostrarAlIniciar = new CheckBox("Mostrar al iniciar");
        mostrarAlIniciar.setSelected(alIniciar);
        mostrarAlIniciar.setOnAction(e -> alCambiarNoMostrar.accept(!mostrarAlIniciar.isSelected()));
        Button agenda = Ui.primary("agenda", "Ver agenda");
        agenda.setOnAction(e -> { d.close(); irAgenda.run(); });
        Button ok = Ui.secondary(null, "Entendido");
        ok.setOnAction(e -> d.close());
        HBox pie = new HBox(10, mostrarAlIniciar, Ui.spacer(), ok, agenda);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.getStyleClass().add("dialog-footer");

        VBox raiz = new VBox(sp, pie);
        raiz.getStyleClass().add("app-root");
        Scene sc = new Scene(raiz, 500, 580);
        ThemeService.get().applyTo(sc);
        d.setScene(sc);
        d.show();
        d.toFront();
        Anim.pageIn(cuerpo);
    }

    private static VBox seccion(String titulo, String icon, String tono) {
        Label t = Ui.label(titulo, "h3");
        HBox encabezado = new HBox(10, Ui.iconChip(icon, tono, 30, 16), t);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox s = new VBox(8, encabezado);
        return s;
    }

    private static HBox fila(String cuando, String texto, String tono, boolean destacada) {
        Label c = Ui.label(cuando, "summary-when", "tint-text-" + tono);
        c.setMinWidth(130);
        Label t = Ui.label(texto, "row-title");
        t.setWrapText(true);
        HBox h = new HBox(10, c, t);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add(destacada ? "summary-row-hot" : "summary-row");
        if (destacada) h.getStyleClass().add("chip-" + tono);
        return h;
    }

    private static HBox filaEvento(Evento e) {
        return fila(e.getHoraInicio() + " – " + e.getHoraFin(), e.getTitulo(), "teal", false);
    }
}
