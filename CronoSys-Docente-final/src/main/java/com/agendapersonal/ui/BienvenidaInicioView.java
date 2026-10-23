package com.agendapersonal.ui;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.infrastructure.repo.NotaDAO;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.util.Perfil;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Saludo al abrir la aplicación, con un recuerdo tomado del contenido que la persona guardó antes. */
public final class BienvenidaInicioView {
    private record Recuerdo(String etiqueta, String texto) {}

    private BienvenidaInicioView() {}

    public static void mostrar(Window owner, MainController controller, Runnable abrirAgenda, Runnable abrirNotas, Runnable abrirEspacioDocente) {
        Stage popup = new Stage();
        if (owner != null && owner.isShowing()) popup.initOwner(owner);
        popup.setTitle("Compás — Bienvenida");
        Logo.aplicarIconos(popup);

        boolean docente = com.agendapersonal.util.Perfil.esDocente();
        Label saludo = Ui.label(Perfil.saludo(LocalTime.now().getHour(), Perfil.nombre()) + " 👋", "h1");
        Label sub = Ui.muted(docente
            ? "Qué bueno verte. Aquí va un pequeño recuerdo de tu espacio y lo que prepara tu docente hoy."
            : "Qué bueno verte. Este es un pequeño recuerdo de tu espacio.");
        sub.setWrapText(true);
        VBox encabezado = new VBox(5, Logo.mark(48), saludo, sub);
        encabezado.setAlignment(Pos.CENTER);

        Recuerdo recuerdo = recuerdo(controller);
        Label etiqueta = Ui.label(recuerdo.etiqueta(), "caption");
        Label texto = Ui.label(recuerdo.texto(), "h3");
        texto.setWrapText(true);
        VBox tarjeta = Ui.card();
        tarjeta.getChildren().addAll(Ui.iconChip("sparkle", "orange", 34, 18), etiqueta, texto);

        Button notas = Ui.secondary("notes", "Abrir notas");
        notas.setOnAction(e -> { popup.close(); abrirNotas.run(); });
        Button agenda = Ui.primary("agenda", "Continuar");
        agenda.setOnAction(e -> { popup.close(); abrirAgenda.run(); });
        Button docenteBtn = null;
        if (docente) {
            docenteBtn = Ui.secondary("book", "Espacio Docente");
            docenteBtn.setOnAction(e -> { popup.close(); abrirEspacioDocente.run(); });
        }
        HBox acciones = new HBox(10);
        acciones.getChildren().addAll(notas, Ui.spacer(), agenda);
        if (docente) acciones.getChildren().addAll(Ui.spacer(), docenteBtn);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox raiz = new VBox(18, encabezado, tarjeta, acciones);
        raiz.setPadding(new Insets(26));
        raiz.setPrefWidth(440);
        raiz.getStyleClass().add("page");
        Scene scene = new Scene(raiz, 460, 380);
        ThemeService.get().applyTo(scene);
        popup.setScene(scene);
        popup.setResizable(false);
        popup.show();
        popup.toFront();
    }

    private static Recuerdo recuerdo(MainController controller) {
        List<Nota> notas;
        try { notas = new NotaDAO().listar(null); } catch (RuntimeException e) { notas = List.of(); }
        Nota nota = notas.stream().max(Comparator.comparing(BienvenidaInicioView::fechaNota,
            Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (nota != null) {
            String detalle = nota.getContenido() == null ? "" : nota.getContenido().replaceAll("\\s+", " ").trim();
            if (detalle.length() > 150) detalle = detalle.substring(0, 147) + "…";
            return new Recuerdo("Una nota que guardaste", detalle.isBlank() ? nota.getTitulo() : nota.getTitulo() + " — " + detalle);
        }

        Tarea completada = controller.getTareas().stream().filter(Tarea::estaCompletada)
            .max(Comparator.comparing(Tarea::getFecha, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (completada != null) return new Recuerdo("Un logro anterior", "Completaste: " + completada.getTitulo());

        LocalDate hoy = LocalDate.now();
        Evento anterior = controller.getEventos().stream().filter(e -> e.getFecha() != null && e.getFecha().isBefore(hoy))
            .max(Comparator.comparing(Evento::getFecha)).orElse(null);
        if (anterior != null) return new Recuerdo("Un evento anterior", anterior.getFecha() + " · " + anterior.getTitulo());

        return new Recuerdo("Tu primer recuerdo", "Cuando guardes una nota, completes una tarea o registres un evento, Compás te lo recordará aquí.");
    }

    private static LocalDateTime fechaNota(Nota nota) {
        String valor = nota.getFechaUpdate() == null || nota.getFechaUpdate().isBlank()
            ? nota.getFechaCreacion() : nota.getFechaUpdate();
        if (valor == null || valor.isBlank()) return null;
        try { return LocalDateTime.parse(valor); }
        catch (DateTimeParseException ignored) {
            try { return LocalDate.parse(valor).atStartOfDay(); }
            catch (DateTimeParseException ignoredAgain) { return null; }
        }
    }
}
