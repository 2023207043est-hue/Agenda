package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.BloqueTiempo;
import com.agendapersonal.domain.model.Habito;
import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.infrastructure.repo.BloqueDAO;
import com.agendapersonal.infrastructure.repo.HabitoDAO;
import com.agendapersonal.infrastructure.repo.NotaDAO;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.AsistenteLocal;
import com.agendapersonal.service.ListaPendientes;
import com.agendapersonal.service.MetodoEnfoque;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Espacio de estudio: un panel a medida con los trozos que tú eliges (enfoque rápido, sugerencias
 * del asistente local, pendientes, bloques del día, hábitos, notas y un resumen del día).
 *
 * <p>Todo funciona sin internet: el asistente lee tus datos y te propone por dónde empezar.</p>
 */
public class EspacioEstudioView {

    /** Los paneles disponibles. El orden en que los guardes es el orden en que se ven. */
    private enum Panel {
        ENFOQUE("enfoque", "Enfoque rápido", "clock"),
        SUGERENCIAS("sugerencias", "Sugerencias", "sparkle"),
        PENDIENTES("pendientes", "Pendientes", "tasks"),
        BLOQUES("bloques", "Bloques de hoy", "calendar"),
        HABITOS("habitos", "Hábitos de hoy", "habits"),
        NOTAS("notas", "Notas rápidas", "notes"),
        RESUMEN("resumen", "Tu día en números", "activity");

        final String id, titulo, icono;
        Panel(String id, String titulo, String icono) { this.id = id; this.titulo = titulo; this.icono = icono; }

        static Panel porId(String id) {
            for (Panel p : values()) if (p.id.equals(id)) return p;
            return null;
        }
    }

    private final MainController c;
    private final NotaDAO notas = new NotaDAO();
    private final BloqueDAO bloques = new BloqueDAO();
    private final HabitoDAO habitos = new HabitoDAO();

    private final VBox view = Ui.page();
    private final VBox zonaPaneles = new VBox(18);
    private final VBox zonaPersonalizar = new VBox(10);
    private ComboBox<MetodoEnfoque> metodoRapido;

    public EspacioEstudioView(MainController c) {
        this.c = c;
        Button personalizar = Ui.secondary("settings", "Personalizar el espacio");
        personalizar.setOnAction(e -> alternarPersonalizar());
        view.getChildren().add(Ui.pageHeader("book", "Espacio de estudio",
            "Tu panel de trabajo: elige los bloques que quieres ver y el orden en que aparecen.", personalizar));

        zonaPersonalizar.getStyleClass().addAll("card");
        zonaPersonalizar.setVisible(false);
        zonaPersonalizar.setManaged(false);

        view.getChildren().addAll(zonaPersonalizar, zonaPaneles);
        recargar();
    }

    private com.agendapersonal.settings.SettingsStore st() { return ThemeService.get().settings(); }

    // ------------------------------------------------------------------ paneles

    private List<Panel> panelesActivos() {
        List<String> guardados = st().strings("study.widgets");
        List<Panel> salida = new ArrayList<>();
        for (String id : guardados) {
            Panel p = Panel.porId(id);
            if (p != null && !salida.contains(p)) salida.add(p);
        }
        if (salida.isEmpty()) {                       // la primera vez se muestran los principales
            salida.addAll(List.of(Panel.ENFOQUE, Panel.SUGERENCIAS, Panel.PENDIENTES, Panel.BLOQUES));
        }
        return salida;
    }

    private void guardarPaneles(List<Panel> activos) {
        List<Object> ids = new ArrayList<>();
        for (Panel p : activos) ids.add(p.id);
        st().set("study.widgets", ids);
    }

    private void recargar() {
        zonaPaneles.getChildren().clear();
        for (Panel p : panelesActivos()) {
            Node n = construir(p);
            if (n != null) zonaPaneles.getChildren().add(n);
        }
        if (zonaPersonalizar.isVisible()) pintarPersonalizar();
    }

    private Node construir(Panel p) {
        return switch (p) {
            case ENFOQUE -> panelEnfoque();
            case SUGERENCIAS -> panelSugerencias();
            case PENDIENTES -> panelPendientes();
            case BLOQUES -> panelBloques();
            case HABITOS -> panelHabitos();
            case NOTAS -> panelNotas();
            case RESUMEN -> panelResumen();
        };
    }

    private VBox tarjeta(String titulo, String icono, Node... contenido) {
        HBox head = new HBox(10, Ui.iconChip(icono, "orange", 30, 16), Ui.h3(titulo));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox card = Ui.card();
        card.getChildren().addAll(head);
        card.getChildren().addAll(contenido);
        return card;
    }

    // ------------------------------------------------------------------ Enfoque rápido

    private VBox panelEnfoque() {
        List<MetodoEnfoque> propios = new ArrayList<>(MetodoEnfoque.desdeMapas(st().value("timeBlocking.presets")));
        metodoRapido = new ComboBox<>();
        metodoRapido.getItems().addAll(MetodoEnfoque.PREDEFINIDOS);
        metodoRapido.getItems().addAll(propios);
        metodoRapido.setValue(metodoRapido.getItems().get(0));
        metodoRapido.setMaxWidth(Double.MAX_VALUE);
        javafx.util.Callback<javafx.scene.control.ListView<MetodoEnfoque>, javafx.scene.control.ListCell<MetodoEnfoque>> celdas =
            lv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(MetodoEnfoque m, boolean empty) {
                    super.updateItem(m, empty);
                    setText(empty || m == null ? null : m.nombre());
                }
            };
        metodoRapido.setCellFactory(celdas);
        metodoRapido.setButtonCell(celdas.call(null));   // si no, el desplegable cerrado muestra el registro entero

        Label resumen = Ui.label("", "caption");
        TextField enfocar = new TextField();
        enfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");
        Button iniciar = Ui.primary("play", "Empezar");
        iniciar.setOnAction(e -> {
            MetodoEnfoque m = metodoRapido.getValue();
            if (m == null) return;
            List<ListaPendientes.Item> items = pendientesDeHoy();
            PantallaEnfoque.abrir(m, enfocar.getText(),
                PantallaEnfoque.Importancia.desde(st().str("timeBlocking.importance")), items, this::alternarPendiente);
        });

        Runnable pintar = () -> {
            MetodoEnfoque m = metodoRapido.getValue();
            resumen.setText(m == null ? "" : m.resumen());
        };
        metodoRapido.setOnAction(e -> pintar.run());
        pintar.run();

        VBox card = tarjeta("Enfoque rápido", "clock",
            Ui.muted("Arranca una sesión sin salir de aquí: reloj, progreso y tus pendientes."),
            Ui.field("Método", metodoRapido), resumen,
            Ui.field("Enfoque", enfocar),
            new HBox(10, iniciar));
        return card;
    }

    // ------------------------------------------------------------------ Sugerencias

    private VBox panelSugerencias() {
        AsistenteLocal.Contexto ctx = new AsistenteLocal.Contexto(
            new ArrayList<>(c.getTareas()), notas.listar(null), bloques.porFecha(LocalDate.now().toString()),
            habitos.listar(), habitos.cumplidos(LocalDate.now().toString()), LocalDate.now());
        List<AsistenteLocal.Sugerencia> sugerencias = AsistenteLocal.sugerir(ctx);

        VBox lista = new VBox(8);
        for (AsistenteLocal.Sugerencia s : sugerencias) {
            HBox fila = new HBox(12, Ui.iconChip("idea", s.tono(), 30, 16),
                new VBox(2, Ui.label(s.titulo(), "row-title"), Ui.muted(s.detalle())));
            fila.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(fila.getChildren().get(1), Priority.ALWAYS);
            fila.getStyleClass().add("estudio-sugerencia");
            lista.getChildren().add(fila);
        }
        VBox card = tarjeta("Sugerencias", "sparkle",
            Ui.muted("Un asistente que funciona en tu equipo: lee tus tareas, notas, bloques y hábitos "
                + "y te dice por dónde empezar. Sin internet y sin enviar nada."),
            lista);
        return card;
    }

    // ------------------------------------------------------------------ Pendientes

    private VBox panelPendientes() {
        List<ListaPendientes.Item> items = pendientesDeHoy();
        VBox lista = new VBox(6);
        if (items.isEmpty()) {
            lista.getChildren().add(Ui.muted("No tienes pendientes para hoy. Añade casillas a tus notas o crea tareas."));
        }
        for (ListaPendientes.Item it : items) {
            CheckBox cb = new CheckBox(it.texto());
            cb.setSelected(it.hecho());
            cb.setWrapText(true);
            cb.getStyleClass().add("estudio-pendiente");
            cb.setOnAction(e -> {
                boolean hecho = cb.isSelected();
                alternarPendiente(it, hecho);
                if (hecho) cb.getStyleClass().add("tachado");
                else cb.getStyleClass().remove("tachado");
            });
            if (it.hecho()) cb.getStyleClass().add("tachado");
            lista.getChildren().add(cb);
        }
        VBox card = tarjeta("Pendientes", "tasks",
            Ui.muted("Lo que tienes pendiente hoy: tareas de la agenda y casillas de tus notas."), lista);
        return card;
    }

    private List<ListaPendientes.Item> pendientesDeHoy() {
        List<ListaPendientes.Item> lista = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (Tarea t : c.getTareas()) {
            if (t.getEstadoTipo() == null || t.getEstadoTipo().esCompletada()) continue;
            if (t.getFecha() != null && t.getFecha().isAfter(hoy)) continue;
            lista.add(new ListaPendientes.Item(t.getId(), -1, t.getTitulo(), false, "tarea"));
        }
        lista.addAll(ListaPendientes.desdeNotas(notas.listar(null)));
        return lista;
    }

    private void alternarPendiente(ListaPendientes.Item item, boolean hecho) {
        if ("tarea".equals(item.origen())) {
            for (Tarea t : c.getTareas())
                if (t.getId() == item.notaId()) { c.alternarCompletada(t); return; }
            return;
        }
        for (Nota n : notas.listar(null)) {
            if (n.getId() != item.notaId()) continue;
            String contenido = ListaPendientes.alternar(n, item, hecho);
            if (contenido.equals(n.getContenido())) return;
            n.setContenido(contenido);
            notas.actualizar(n);
            return;
        }
    }

    // ------------------------------------------------------------------ Bloques de hoy

    private VBox panelBloques() {
        List<BloqueTiempo> delDia = bloques.porFecha(LocalDate.now().toString());
        VBox lista = new VBox(6);
        if (delDia.isEmpty()) lista.getChildren().add(Ui.muted("Hoy no tienes bloques reservados."));
        for (BloqueTiempo b : delDia) {
            HBox fila = new HBox(10, Ui.label(b.getHoraInicio() + " – " + b.getHoraFin(), "time-range"),
                Ui.label(b.getTitulo(), "row-title"));
            fila.setAlignment(Pos.CENTER_LEFT);
            lista.getChildren().add(fila);
        }
        return tarjeta("Bloques de hoy", "calendar",
            Ui.muted("Lo que ya tiene hora hoy."), lista);
    }

    // ------------------------------------------------------------------ Hábitos

    private VBox panelHabitos() {
        List<Habito> lista = habitos.listar();
        Set<Integer> cumplidos = habitos.cumplidos(LocalDate.now().toString());
        VBox filas = new VBox(6);
        if (lista.isEmpty()) filas.getChildren().add(Ui.muted("Todavía no tienes hábitos."));
        for (Habito h : lista) {
            CheckBox cb = new CheckBox(h.getNombre());
            cb.setSelected(cumplidos.contains(h.getId()));
            cb.setOnAction(e -> {
                if (cb.isSelected()) habitos.marcar(h.getId(), LocalDate.now().toString());
                else habitos.desmarcar(h.getId(), LocalDate.now().toString());
            });
            filas.getChildren().add(cb);
        }
        return tarjeta("Hábitos de hoy", "habits", filas);
    }

    // ------------------------------------------------------------------ Notas rápidas

    private VBox panelNotas() {
        TextField campo = new TextField();
        campo.setPromptText("Escribe una idea y pulsa Enter…");
        HBox.setHgrow(campo, Priority.ALWAYS);
        Button guardar = Ui.secondary(null, "Guardar");
        Runnable add = () -> {
            String txt = campo.getText().trim();
            if (txt.isEmpty()) { Anim.shake(campo); return; }
            String hoy = LocalDate.now().toString();
            if (notas.insertar(new Nota(0, txt, "", "idea", "IDEA", hoy, hoy, false))) {
                campo.clear();
                Notifier.success("Idea guardada", txt);
                recargar();
            } else Notifier.error("No se pudo guardar la idea", null);
        };
        guardar.setOnAction(e -> add.run());
        campo.setOnAction(e -> add.run());

        List<Nota> recientes = notas.listar(null);
        VBox lista = new VBox(4);
        for (Nota n : recientes.subList(0, Math.min(5, recientes.size())))
            lista.getChildren().add(Ui.label("• " + n.getTitulo(), "caption"));

        return tarjeta("Notas rápidas", "notes",
            new HBox(8, campo, guardar), lista);
    }

    // ------------------------------------------------------------------ Resumen

    private VBox panelResumen() {
        LocalDate hoy = LocalDate.now();
        int hechas = 0, pendientes = 0;
        for (Tarea t : c.getTareas()) {
            if (t.getEstadoTipo() == null) continue;
            if (t.getEstadoTipo().esCompletada()) hechas++; else pendientes++;
        }
        Set<Integer> cumplidos = habitos.cumplidos(hoy.toString());
        int habitosTotal = habitos.listar().size();
        int bloquesHoy = bloques.porFecha(hoy.toString()).size();

        HBox fila = new HBox(16,
            Ui.stat("Tareas hechas", String.valueOf(hechas), "de " + (hechas + pendientes) + " en total", "success", "green"),
            Ui.stat("Hábitos de hoy", cumplidos.size() + " / " + habitosTotal, "marcados", "habits", "orange"),
            Ui.stat("Bloques hoy", String.valueOf(bloquesHoy), "reservados", "calendar", "blue"));
        HBox.setHgrow(fila.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(fila.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(fila.getChildren().get(2), Priority.ALWAYS);
        return tarjeta("Tu día en números", "activity", fila);
    }

    // ------------------------------------------------------------------ personalizar

    private void alternarPersonalizar() {
        boolean visible = !zonaPersonalizar.isVisible();
        zonaPersonalizar.setVisible(visible);
        zonaPersonalizar.setManaged(visible);
        if (visible) pintarPersonalizar();
    }

    private void pintarPersonalizar() {
        zonaPersonalizar.getChildren().clear();
        zonaPersonalizar.getChildren().addAll(Ui.h3("Personaliza tu espacio"),
            Ui.muted("Marca los paneles que quieres ver y ordénalos con las flechas. Se guarda en tu perfil."));
        List<Panel> activos = panelesActivos();
        for (Panel p : Panel.values()) {
            boolean activo = activos.contains(p);
            CheckBox cb = new CheckBox(p.titulo);
            cb.setSelected(activo);
            cb.setOnAction(e -> {
                List<Panel> lista = new ArrayList<>(panelesActivos());
                if (cb.isSelected()) { if (!lista.contains(p)) lista.add(p); }
                else lista.remove(p);
                guardarPaneles(lista);
                recargar();
            });
            HBox fila = new HBox(10, cb, Ui.spacer());
            if (activo) {
                Button arriba = Ui.iconButton("chevron-up", "Subir");
                Button abajo = Ui.iconButton("chevron-down", "Bajar");
                arriba.setOnAction(e -> mover(p, -1));
                abajo.setOnAction(e -> mover(p, 1));
                fila.getChildren().addAll(arriba, abajo);
            }
            fila.setAlignment(Pos.CENTER_LEFT);
            zonaPersonalizar.getChildren().add(fila);
        }
        Button cerrar = Ui.secondary(null, "Listo");
        cerrar.setOnAction(e -> alternarPersonalizar());
        zonaPersonalizar.getChildren().add(cerrar);
    }

    private void mover(Panel p, int delta) {
        List<Panel> lista = new ArrayList<>(panelesActivos());
        int i = lista.indexOf(p);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= lista.size()) return;
        lista.remove(i);
        lista.add(j, p);
        guardarPaneles(lista);
        recargar();
    }

    public Node getView() { return Ui.scroll(view); }
}
