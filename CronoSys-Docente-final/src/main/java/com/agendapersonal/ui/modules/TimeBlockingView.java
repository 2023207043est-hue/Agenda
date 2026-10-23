package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.BloqueTiempo;
import com.agendapersonal.domain.model.Nota;
import com.agendapersonal.infrastructure.repo.BloqueDAO;
import com.agendapersonal.infrastructure.repo.NotaDAO;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.service.ListaPendientes;
import com.agendapersonal.service.MetodoEnfoque;
import com.agendapersonal.ui.Anim;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.ui.Switch;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Time blocking: reserva bloques de enfoque por día, avisa si chocan con otros bloques o eventos,
 * y configura la sesión de enfoque (método, duración, importancia, pendientes y fondo de la pantalla).
 */
public class TimeBlockingView {
    private static final Map<String, String> TONO = Map.of(
        "Enfoque", "orange", "Reunión", "blue", "Estudio", "violet", "Salud", "green", "Personal", "rose");

    private final MainController c;
    private final BloqueDAO dao = new BloqueDAO();
    private final NotaDAO notas = new NotaDAO();
    private final VBox view;
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final VBox lista = new VBox(0);
    private final Label tituloDia = Ui.label("", "h3");

    /** Métodos propios guardados en el perfil. */
    private List<MetodoEnfoque> propios = new ArrayList<>();
    private ComboBox<MetodoEnfoque> metodos;
    private Label descripcion, uso, resumen;
    private TextField enfocar;

    public TimeBlockingView(MainController c) {
        this.c = c;
        this.propios = new ArrayList<>(MetodoEnfoque.desdeMapas(ThemeService.get().settings().value("timeBlocking.presets")));

        VBox page = Ui.page();
        Button prev = Ui.iconButton("chevron-left", "Día anterior");
        Button next = Ui.iconButton("chevron-right", "Día siguiente");
        Button hoy = Ui.secondary(null, "Hoy");
        prev.setOnAction(e -> fecha.setValue(fecha.getValue().minusDays(1)));
        next.setOnAction(e -> fecha.setValue(fecha.getValue().plusDays(1)));
        hoy.setOnAction(e -> fecha.setValue(LocalDate.now()));
        Ui.fechaCorta(fecha);
        fecha.setOnAction(e -> fill());
        fecha.setPrefWidth(170);
        HBox nav = new HBox(6, prev, fecha, next, hoy);
        nav.setAlignment(Pos.CENTER_RIGHT);
        page.getChildren().add(Ui.pageHeader("clock", "Time blocking",
            "Reserva bloques de enfoque por día y arranca sesiones con reloj, progreso y lista de pendientes.", nav));

        page.getChildren().addAll(cardSesion(), cardMetodos(), cardFondo(), cardNuevoBloque(), cardLista());

        view = page;
        fill();
    }

    // ------------------------------------------------------------------ sesión de enfoque

    private VBox cardSesion() {
        metodos = new ComboBox<>();
        metodos.getItems().addAll(metodosBase());
        metodos.getItems().addAll(propios);
        metodos.setValue(metodos.getItems().get(0));
        metodos.setCellFactory(fabricaCelda());
        metodos.setButtonCell(fabricaCelda().call(null));
        metodos.setMaxWidth(Double.MAX_VALUE);

        descripcion = Ui.muted("");
        uso = Ui.muted("");
        resumen = Ui.label("", "caption");
        resumen.getStyleClass().add("caption");
        metodos.setOnAction(e -> verMetodo());
        verMetodo();

        enfocar = new TextField();
        enfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");

        // Importancia de la sesión
        ComboBox<PantallaEnfoque.Importancia> imp = new ComboBox<>();
        imp.getItems().setAll(PantallaEnfoque.Importancia.values());
        imp.setValue(PantallaEnfoque.Importancia.desde(st().str("timeBlocking.importance")));
        imp.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PantallaEnfoque.Importancia i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.toString());
                setTooltip(empty || i == null ? null : new javafx.scene.control.Tooltip(i.ayuda()));
            }
        });
        imp.setOnAction(e -> {
            if (imp.getValue() == null) return;
            st().set("timeBlocking.importance", imp.getValue().name());
        });

        // De dónde salen los pendientes
        String[] fuentes = {"ambas", "tareas", "notas", "ninguna"};
        Ui.Segmented origen = new Ui.Segmented("Tareas y notas", "Solo tareas", "Solo notas", "Ninguna");
        int idx = indexOf(fuentes, st().str("timeBlocking.todoSource"));
        origen.select(idx < 0 ? 0 : idx);
        origen.selectedProperty().addListener((o, a, v) -> {
            st().set("timeBlocking.todoSource", fuentes[v.intValue()]);
            resumenPendientes();
        });

        Switch verPendientes = new Switch("Mostrar la lista de pendientes en la pantalla",
            "Puedes marcar las casillas mientras trabajas: se guarda en tus tareas o en tus notas.",
            st().bool("timeBlocking.showTasks"));
        verPendientes.selectedProperty().addListener((o, a, v) -> st().set("timeBlocking.showTasks", v));

        Switch pausar = new Switch("Pausar el contador si salgo de la pantalla",
            "Se aplica siempre con la importancia Alta o Estricta.", st().bool("timeBlocking.pauseOnLeave"));
        pausar.selectedProperty().addListener((o, a, v) -> st().set("timeBlocking.pauseOnLeave", v));

        Button iniciar = Ui.primary("clock", "Iniciar enfoque");
        iniciar.setOnAction(e -> iniciar());

        HBox botonera = new HBox(10, iniciar, Ui.muted("También puedes cambiar el fondo y los ajustes dentro de la pantalla."));
        botonera.setAlignment(Pos.CENTER_LEFT);

        VBox card = Ui.card();
        card.getChildren().addAll(
            Ui.h3("Sesión de enfoque"),
            Ui.muted("Elige un método, la importancia y los pendientes. Se abre una pantalla con reloj, "
                + "barra de progreso, número de sesión y botones de control."),
            Ui.field("Método", metodos), descripcion, uso, resumen,
            Ui.field("Enfoque", enfocar),
            Ui.field("Importancia de la sesión", imp),
            Ui.field("Pendientes de la sesión", origen.node()),
            verPendientes, pausar,
            botonera);
        return card;
    }

    private javafx.util.Callback<ListView<MetodoEnfoque>, ListCell<MetodoEnfoque>> fabricaCelda() {
        return lv -> new ListCell<>() {
            @Override protected void updateItem(MetodoEnfoque m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setText(null); return; }
                setText(m.nombre() + (m.propio() ? "  · propio" : ""));
                setTooltip(new javafx.scene.control.Tooltip(m.descripcion()));
            }
        };
    }

    private void verMetodo() {
        MetodoEnfoque m = metodos.getValue();
        if (m == null) return;
        descripcion.setText(m.descripcion());
        uso.setText("Uso común: " + m.usoComun());
        resumen.setText(m.resumen() + (m.tieneRangos() ? "  ·  la duración concreta se sortea al empezar" : ""));
        resumenPendientes();
    }

    /** Añade al resumen cuántos pendientes habrá en la pantalla de bloqueo. */
    private void resumenPendientes() {
        List<ListaPendientes.Item> items = pendientes();
        if (items.isEmpty()) return;
        long hechos = items.stream().filter(ListaPendientes.Item::hecho).count();
        resumen.setText(resumen.getText() + "  ·  " + hechos + " de " + items.size() + " pendientes listos");
    }

    /** Los pendientes que se mostrarán en la pantalla de bloqueo. */
    private List<ListaPendientes.Item> pendientes() {
        List<ListaPendientes.Item> lista = new ArrayList<>();
        String fuente = st().str("timeBlocking.todoSource");
        if ("ninguna".equals(fuente)) return lista;
        if ("tareas".equals(fuente) || "ambas".equals(fuente)) {
            LocalDate hoy = LocalDate.now();
            for (Tarea t : c.getTareas()) {
                if (t.getEstadoTipo() == null || t.getEstadoTipo().esCompletada()) continue;
                if (t.getFecha() != null && t.getFecha().isAfter(hoy)) continue;   // solo lo de hoy y lo vencido
                lista.add(new ListaPendientes.Item(t.getId(), -1, t.getTitulo(), false, "tarea"));
            }
        }
        if ("notas".equals(fuente) || "ambas".equals(fuente)) {
            lista.addAll(ListaPendientes.desdeNotas(notas.listar(null)));
        }
        return lista;
    }

    private void iniciar() {
        MetodoEnfoque m = metodos.getValue();
        if (m == null) return;
        List<ListaPendientes.Item> items = pendientes();
        PantallaEnfoque.Importancia imp = PantallaEnfoque.Importancia.desde(st().str("timeBlocking.importance"));
        PantallaEnfoque.abrir(m, enfocar.getText(), imp, items, this::alternarPendiente);
    }

    /** Guarda el cambio de un pendiente (tarea completada o casilla de una nota marcada). */
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
            if (notas.actualizar(n)) Notifier.success(hecho ? "Pendiente hecho" : "Pendiente devuelto", item.texto());
            return;
        }
    }

    // ------------------------------------------------------------------ métodos

    private List<MetodoEnfoque> metodosBase() {
        List<MetodoEnfoque> base = new ArrayList<>(MetodoEnfoque.PREDEFINIDOS);
        if (com.agendapersonal.util.Perfil.esDocente()) base.addAll(MetodoEnfoque.DOCENTES);
        return base;
    }

    private VBox cardMetodos() {
        VBox listaMetodos = new VBox(8);

        for (MetodoEnfoque m : metodosBase()) {
            listaMetodos.getChildren().add(filaMetodo(m, false));
        }
        for (MetodoEnfoque m : propios) {
            listaMetodos.getChildren().add(filaMetodo(m, true));
        }

        // Crear el tuyo (con rangos)
        TextField nom = new TextField();
        nom.setPromptText("Nombre del método");
        TextField trabajo = new TextField("30"), trabajoMax = new TextField("30");
        TextField descanso = new TextField("5"), descansoMax = new TextField("5");
        TextField sesiones = new TextField("4"), largo = new TextField("15"), largoMax = new TextField("15");
        for (TextField t : List.of(trabajo, trabajoMax, descanso, descansoMax, sesiones, largo, largoMax)) {
            t.setPrefColumnCount(4);
        }
        Button crear = Ui.primary("add", "Guardar método propio");
        crear.setOnAction(e -> crearMetodo(nom, trabajo, trabajoMax, descanso, descansoMax, sesiones, largo, largoMax));

        VBox grupoTrabajo = new VBox(4, Ui.label("Trabajo (min)", "caption"), new HBox(6, trabajo, Ui.label("a", "caption"), trabajoMax));
        VBox grupoDescanso = new VBox(4, Ui.label("Descanso (min)", "caption"), new HBox(6, descanso, Ui.label("a", "caption"), descansoMax));
        VBox grupoSesiones = new VBox(4, Ui.label("Sesiones por ciclo", "caption"), sesiones);
        VBox grupoLargo = new VBox(4, Ui.label("Descanso largo (min)", "caption"), new HBox(6, largo, Ui.label("a", "caption"), largoMax));

        HBox formulario = new HBox(12, Ui.field("Nombre", nom), grupoTrabajo, grupoDescanso, grupoSesiones, grupoLargo, crear);
        formulario.setAlignment(Pos.BOTTOM_LEFT);

        VBox card = Ui.card();
        card.getChildren().addAll(Ui.h3("Métodos de bloqueo"),
            Ui.muted("Cada método tiene su descripción y su uso común. Los tiempos pueden ser un valor fijo "
                + "o un rango (por ejemplo 60 a 90 minutos): al empezar la sesión se sortea uno dentro del rango."),
            listaMetodos, Ui.field("Crea el tuyo", formulario));
        return card;
    }

    private Node filaMetodo(MetodoEnfoque m, boolean propio) {
        Label n = Ui.label(m.nombre(), "row-title");
        Label d = Ui.muted(m.descripcion());
        d.setMaxWidth(420);
        Label u = Ui.label("Uso común: " + m.usoComun(), "caption");
        VBox textos = new VBox(3, n, d, u);
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox acciones = new HBox(8);
        Button usar = Ui.secondary(null, "Usar");
        usar.setOnAction(e -> { metodos.setValue(m); verMetodo(); });
        acciones.getChildren().add(usar);
        if (propio) {
            Button dup = Ui.iconButton("copy", "Duplicar este método");
            dup.setOnAction(e -> duplicar(m));
            Button del = Ui.iconButton("delete", "Eliminar este método");
            del.setOnAction(e -> eliminarMetodo(m));
            acciones.getChildren().addAll(dup, del);
        }
        HBox fila = new HBox(14, textos, acciones);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().addAll("list-row", "with-divider");
        return fila;
    }

    private void crearMetodo(TextField nom, TextField trabajo, TextField trabajoMax, TextField descanso,
                            TextField descansoMax, TextField sesiones, TextField largo, TextField largoMax) {
        try {
            int t1 = Integer.parseInt(trabajo.getText().trim());
            int t2 = Integer.parseInt(trabajoMax.getText().trim());
            int d1 = Integer.parseInt(descanso.getText().trim());
            int d2 = Integer.parseInt(descansoMax.getText().trim());
            int s = Integer.parseInt(sesiones.getText().trim());
            int l1 = Integer.parseInt(largo.getText().trim());
            int l2 = Integer.parseInt(largoMax.getText().trim());
            String nombre = nom.getText().trim();
            if (nombre.isEmpty()) { Anim.shake(nom); Notifier.warning("Falta el nombre", "Ponle un nombre a tu método."); return; }
            MetodoEnfoque m = MetodoEnfoque.personalizado(nombre, t1, t2, d1, d2, s, l1, l2);
            propios.add(m);
            guardarPropios();
            metodos.getItems().add(m);
            metodos.setValue(m);
            verMetodo();
            nom.clear();
            Notifier.success("Método guardado", m.nombre() + " · " + m.resumen());
        } catch (NumberFormatException ex) {
            Notifier.warning("Revisa los números", "Usa minutos enteros, por ejemplo 40 y 8.");
        }
    }

    private void duplicar(MetodoEnfoque m) {
        MetodoEnfoque copia = m.comoPropio(m.nombre() + " (copia)");
        propios.add(copia);
        guardarPropios();
        metodos.getItems().add(copia);
        Notifier.success("Método duplicado", copia.nombre());
    }

    private void eliminarMetodo(MetodoEnfoque m) {
        if (!Ui.confirm(view, "¿Eliminar el método «" + m.nombre() + "»?", "Solo se borra el método, no las sesiones que ya hiciste.", "Eliminar")) return;
        propios.removeIf(x -> x.id().equals(m.id()));
        guardarPropios();
        metodos.getItems().remove(m);
        if (metodos.getValue() == null && !metodos.getItems().isEmpty()) metodos.setValue(metodos.getItems().get(0));
        verMetodo();
        Notifier.deleted("Método eliminado", m.nombre());
    }

    private void guardarPropios() {
        List<Object> mapas = new ArrayList<>();
        for (MetodoEnfoque m : propios) mapas.add(m.aMapa());
        st().set("timeBlocking.presets", mapas);
    }

    // ------------------------------------------------------------------ fondo

    private VBox cardFondo() {
        String[] modos = {"degradado", "archivo", "color"};
        Ui.Segmented fondo = new Ui.Segmented("Degradado del tema", "Imagen o video", "Color");
        fondo.select(Math.max(0, indexOf(modos, st().str("timeBlocking.background"))));
        fondo.selectedProperty().addListener((o, a, v) -> st().set("timeBlocking.background", modos[v.intValue()]));

        Label archivo = Ui.label(nombreFondo(), "caption");
        Button agregarFondo = Ui.secondary("grid", "Agregar fondo…");
        agregarFondo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Fondo de la pantalla de enfoque");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y videos",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.webm", "*.ogv"));
            File f = fc.showOpenDialog(view.getScene() == null ? null : view.getScene().getWindow());
            if (f == null) return;
            st().set("timeBlocking.backgroundFile", f.getAbsolutePath());
            st().set("timeBlocking.background", "archivo");
            fondo.select(1);
            archivo.setText(f.getName());
        });
        Button quitar = Ui.secondary(null, "Quitar");
        quitar.setOnAction(e -> {
            st().set("timeBlocking.backgroundFile", null);
            st().set("timeBlocking.background", "degradado");
            fondo.select(0);
            archivo.setText("Sin archivo");
        });

        ColorPicker colorFondo = new ColorPicker(javafx.scene.paint.Color.web(
            com.agendapersonal.settings.Colors.normalize(st().str("timeBlocking.backgroundColor")) == null
                ? "#1B1E25" : st().str("timeBlocking.backgroundColor")));
        colorFondo.setOnAction(e -> {
            var c1 = colorFondo.getValue();
            st().set("timeBlocking.backgroundColor", com.agendapersonal.settings.Colors.hex(
                (int) Math.round(c1.getRed() * 255), (int) Math.round(c1.getGreen() * 255), (int) Math.round(c1.getBlue() * 255)));
            st().set("timeBlocking.background", "color");
            fondo.select(2);
        });

        Switch repetir = new Switch("Repetir el video en bucle", "Si el fondo es un video, se repite mientras dure la sesión.",
            st().bool("timeBlocking.backgroundLoop"));
        repetir.selectedProperty().addListener((o, a, v) -> st().set("timeBlocking.backgroundLoop", v));

        // Audio local de la sesión (sin internet: un archivo de tu equipo).
        Label audio = Ui.label(st().str("timeBlocking.audioFile").isBlank()
            ? "Sin audio" : new File(st().str("timeBlocking.audioFile")).getName(), "caption");
        Button agregarAudio = Ui.secondary("music", "Elegir audio…");
        agregarAudio.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Audio de la sesión de enfoque");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg",
                "*.m4a", "*.aac", "*.flac", "*.opus"));
            File f = fc.showOpenDialog(view.getScene() == null ? null : view.getScene().getWindow());
            if (f == null) return;
            st().set("timeBlocking.audioFile", f.getAbsolutePath());
            audio.setText(f.getName());
        });
        Button quitarAudio = Ui.secondary(null, "Quitar");
        quitarAudio.setOnAction(e -> {
            st().set("timeBlocking.audioFile", null);
            audio.setText("Sin audio");
        });
        HBox filaAudio = new HBox(10, agregarAudio, audio, quitarAudio);
        filaAudio.setAlignment(Pos.CENTER_LEFT);

        HBox filaFondo = new HBox(10, agregarFondo, archivo, quitar, colorFondo);
        filaFondo.setAlignment(Pos.CENTER_LEFT);

        VBox card = Ui.card();
        card.getChildren().addAll(Ui.h3("Fondo y audio de la pantalla de enfoque"),
            Ui.muted("Puedes cambiarlo también desde la propia pantalla, con los botones «Agregar fondo» y «Ajustes»."),
            Ui.field("Tipo de fondo", fondo.node()),
            filaFondo, repetir,
            Ui.field("Audio de la sesión", filaAudio),
            Ui.muted("Un archivo de audio de tu equipo: se repite mientras trabajas y calla en los descansos."));
        return card;
    }

    private String nombreFondo() {
        String ruta = st().str("timeBlocking.backgroundFile");
        return ruta.isBlank() ? "Sin archivo" : new File(ruta).getName();
    }

    // ------------------------------------------------------------------ bloques del día

    private VBox cardNuevoBloque() {
        TextField titulo = new TextField();
        titulo.setPromptText("Ej. Trabajo profundo en la tesis");
        TextField hi = new TextField("09:00");
        TextField hf = new TextField("11:00");
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll("Enfoque", "Reunión", "Estudio", "Salud", "Personal");
        cat.setValue("Enfoque");
        Button add = Ui.primary("add", "Bloquear");
        Runnable agregar = () -> {
            if (titulo.getText().isBlank()) { Anim.shake(titulo); Notifier.warning("Falta el título", "Ponle un nombre al bloque."); return; }
            LocalTime a, b;
            try { a = LocalTime.parse(hi.getText().trim()); b = LocalTime.parse(hf.getText().trim()); }
            catch (DateTimeParseException ex) { Anim.shake(hi); Notifier.warning("Hora inválida", "Usa el formato HH:mm, por ejemplo 09:30."); return; }
            if (!b.isAfter(a)) { Anim.shake(hf); Notifier.warning("Revisa las horas", "La hora de fin debe ser posterior al inicio."); return; }
            if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
            String f = fecha.getValue().toString();
            String ini = a.toString(), fin = b.toString();
            if (choca(f, ini, fin)) { Notifier.warning("Ese horario ya está ocupado", "Choca con otro bloque o con un evento de tu agenda."); return; }
            if (dao.insertar(new BloqueTiempo(0, titulo.getText().trim(), f, ini, fin, cat.getValue(), null))) {
                Notifier.success("Bloque reservado", titulo.getText().trim() + " · " + ini + "–" + fin);
                titulo.clear();
                fill();
            } else Notifier.error("No se pudo guardar el bloque", null);
        };
        add.setOnAction(e -> agregar.run());
        titulo.setOnAction(e -> agregar.run());
        VBox fT = Ui.field("Bloque", titulo);
        VBox fI = Ui.field("Inicio", hi);
        VBox fF = Ui.field("Fin", hf);
        VBox fC = Ui.field("Categoría", cat);
        fT.setPrefWidth(320); fI.setPrefWidth(110); fF.setPrefWidth(110); fC.setPrefWidth(170);
        HBox fila = new HBox(12, fT, fI, fF, fC, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox form = Ui.card();
        form.getChildren().addAll(Ui.h3("Nuevo bloque"), fila);
        return form;
    }

    private boolean choca(String fecha, String hi, String hf) {
        for (var b : dao.porFecha(fecha))
            if (hi.compareTo(b.getHoraFin()) < 0 && b.getHoraInicio().compareTo(hf) < 0) return true;
        for (var e : c.eventosDelDia(LocalDate.parse(fecha)))
            if (hi.compareTo(e.getHoraFin()) < 0 && e.getHoraInicio().compareTo(hf) < 0) return true;
        return false;
    }

    private VBox cardLista() {
        VBox listaCard = new VBox(0, lista);
        listaCard.getStyleClass().addAll("card", "card-flush");
        return listaCard;
    }

    private void fill() {
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        LocalDate d = fecha.getValue();
        tituloDia.setText(Ui.formatDateShort(d));
        List<BloqueTiempo> bloques = dao.porFecha(d.toString());
        lista.getChildren().clear();
        HBox head = new HBox(8, tituloDia, Ui.spacer(),
            Ui.label(bloques.size() + (bloques.size() == 1 ? " bloque" : " bloques"), "caption"));
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(18, 22, 10, 22));
        lista.getChildren().add(head);
        if (bloques.isEmpty()) {
            lista.getChildren().add(Ui.empty("clock", "Día sin bloques", "Reserva tu primer bloque de enfoque con el formulario de arriba."));
            return;
        }
        List<Node> filas = new ArrayList<>();
        for (BloqueTiempo b : bloques) { Node n = fila(b); lista.getChildren().add(n); filas.add(n); }
        Anim.stagger(filas);
    }

    private Node fila(BloqueTiempo b) {
        String tono = TONO.getOrDefault(b.getCategoria(), "slate");
        Region barra = new Region();
        barra.getStyleClass().addAll("time-bar", "fill-" + tono);
        Label horas = Ui.label(b.getHoraInicio() + " – " + b.getHoraFin(), "time-range");
        horas.setMinWidth(120);
        Label t = Ui.label(b.getTitulo(), "row-title");
        HBox.setHgrow(t, Priority.ALWAYS);
        t.setMaxWidth(Double.MAX_VALUE);
        Button del = Ui.iconButton("delete", "Eliminar bloque");
        del.setOnAction(e -> {
            if (Ui.confirm(view, "¿Eliminar este bloque?", b.getTitulo() + " · " + b.getHoraInicio() + "–" + b.getHoraFin(), "Eliminar")
                && dao.eliminar(b.getId())) {
                fill();
                Notifier.deleted("Bloque eliminado", b.getTitulo());
            }
        });
        HBox row = new HBox(14, barra, horas, t, Ui.pill(b.getCategoria(), tono), del);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("list-row", "with-divider");
        return row;
    }

    // ------------------------------------------------------------------ utilidades

    private com.agendapersonal.settings.SettingsStore st() { return ThemeService.get().settings(); }

    private static int indexOf(String[] arr, String valor) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(valor)) return i;
        return -1;
    }

    public Node getView() { return Ui.scroll(view); }

    /** Fuerza la técnica elegida (p. ej. la preparación de clase desde el inicio del usuario). */
    public void seleccionar(MetodoEnfoque m) { metodos.setValue(m); verMetodo(); }
}
