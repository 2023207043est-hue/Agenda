package com.agendapersonal.ui.modules;

import com.agendapersonal.service.ListaPendientes;
import com.agendapersonal.service.MetodoEnfoque;
import com.agendapersonal.service.SesionEnfoque;
import com.agendapersonal.settings.Colors;
import com.agendapersonal.settings.SettingsStore;
import com.agendapersonal.ui.IconFactory;
import com.agendapersonal.ui.Switch;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Pantalla de bloqueo del tiempo: reloj, barra de progreso, sesión y ciclo, botones de control y
 * lista de pendientes opcional (de tus tareas o de las casillas de tus notas).
 *
 * <p>Todo se puede cambiar <b>desde la propia pantalla</b> (botón «Ajustes»): el tamaño del reloj, qué
 * elementos se ven, el fondo (imagen o video en bucle), el velo, el sonido y la importancia. Los
 * cambios se guardan en tu perfil, así que la próxima vez ya están aplicados.</p>
 *
 * <p><b>Límite honesto:</b> una app de escritorio hecha en Java no puede tapar ni cerrar otros
 * programas del sistema (Windows no lo permite sin permisos de administrador). Lo que sí hace la
 *.importancia <i>Estricta</i> es pantalla completa, ventana siempre al frente, pausa del contador
 * en cuanto sales o minimizas, y bloqueo de los atajos de teclado dentro de la app.</p>
 */
public final class PantallaEnfoque {

    /** Cuánto manda la sesión sobre tu concentración. */
    public enum Importancia {
        NORMAL("Normal", "Puedes salir y volver cuando quieras."),
        ALTA("Alta", "Si minimizas o cambias de ventana, el contador se pausa."),
        ESTRICTA("Estricta", "Pantalla completa, siempre al frente y sin atajos. El contador se pausa al salir.");
        private final String etiqueta, ayuda;
        Importancia(String e, String a) { etiqueta = e; ayuda = a; }
        @Override public String toString() { return etiqueta; }
        public String ayuda() { return ayuda; }
        public static Importancia desde(String v) { try { return valueOf(v); } catch (Exception e) { return NORMAL; } }
    }

    private static final java.util.Set<String> VIDEO = java.util.Set.of("mp4", "m4v", "webm", "ogv");
    private static final java.util.Set<String> IMAGEN = java.util.Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    private final SesionEnfoque sesion;
    private final Stage stage = new Stage();
    private final SettingsStore st = ThemeService.get().settings();

    private final Label fase = new Label(), reloj = new Label(), detalle = new Label(), aviso = new Label();
    private final Label tarea = new Label();
    private final ProgressBar barra = new ProgressBar(0);
    private final VBox listaPendientes = new VBox(8);

    private final Button play = Ui.primary(null, "Iniciar");
    private final Button reiniciar = Ui.secondary(null, "Reiniciar tiempo");
    private final Button siguiente = Ui.secondary(null, "Saltar fase");
    private final Button terminarFlow = Ui.secondary(null, "Terminar sesión");
    private final Button detener = Ui.secondary(null, "Detener");
    private final Button botonFondo = Ui.secondary("grid", "Agregar fondo");
    private final Button botonAjustes = Ui.secondary("settings", "Ajustes");

    private final StackPane raiz = new StackPane();
    private final StackPane capaFondo = new StackPane();
    private final StackPane capaContenido = new StackPane();
    private final Region velo = new Region();
    private final VBox panelAjustes = new VBox(14);
    private final ScrollPane scrollPendientes = new ScrollPane(listaPendientes);

    private MediaPlayer video;
    private MediaPlayer audio;
    private Timeline tic;
    private int salidas;
    private boolean ajustesAbiertos;
    private boolean dialogoAbierto;
    private boolean filtroTeclado;
    private List<ListaPendientes.Item> pendientes = new ArrayList<>();
    private BiConsumer<ListaPendientes.Item, Boolean> alAlternar;
    private Importancia importancia;

    /** Abre la pantalla de bloqueo. {@code alAlternar} guarda los cambios de los pendientes (puede ser null). */
    public static void abrir(MetodoEnfoque metodo, String titulo, Importancia imp, List<ListaPendientes.Item> pendientes,
                             BiConsumer<ListaPendientes.Item, Boolean> alAlternar) {
        new PantallaEnfoque(metodo, titulo, imp, pendientes, alAlternar).stage.show();
    }

    private PantallaEnfoque(MetodoEnfoque metodo, String titulo, Importancia imp,
                            List<ListaPendientes.Item> pendientes, BiConsumer<ListaPendientes.Item, Boolean> alAlternar) {
        this.sesion = new SesionEnfoque(metodo);
        this.importancia = imp == null ? Importancia.NORMAL : imp;
        this.pendientes = pendientes == null ? new ArrayList<>() : new ArrayList<>(pendientes);
        this.alAlternar = alAlternar;

        construirFondo();
        construirContenido(titulo, metodo);

        Scene scene = new Scene(raiz, 1040, 660);
        ThemeService.get().applyTo(scene);
        stage.setScene(scene);
        stage.setTitle("Compás · " + metodo.nombre());
        aplicarImportancia();
        // Al mostrarse se reinscribe el modo Estricta (pantalla completa y al frente).
        stage.setOnShown(e -> { if (importancia == Importancia.ESTRICTA) { stage.toFront(); stage.requestFocus(); } });

        // Salir de la pantalla: minimizar o cambiar de ventana.
        stage.focusedProperty().addListener((o, a, enfocada) -> {
            if (!enfocada) salio();
            else if (importancia == Importancia.ESTRICTA) stage.toFront();
        });
        stage.iconifiedProperty().addListener((o, a, min) -> { if (min) salio(); });

        play.setOnAction(e -> alternar());
        reiniciar.setOnAction(e -> { sesion.reiniciarFase(); aviso.setText(""); actualizar(); });
        siguiente.setOnAction(e -> { sesion.siguiente(); actualizar(); });
        terminarFlow.setOnAction(e -> { sesion.terminarFlow(); sesion.iniciar(); actualizar(); });
        detener.setOnAction(e -> cerrar(true));
        botonFondo.setOnAction(e -> elegirFondo());
        botonAjustes.setOnAction(e -> alternarAjustes());
        stage.setOnCloseRequest(e -> { e.consume(); cerrar(true); });

        tic = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            boolean cambio = sesion.tick(1);
            if (cambio) {
                if (st.bool("timeBlocking.sound")) beep();
                avisarFinDeFase();
            }
            actualizar();
        }));
        tic.setCycleCount(Animation.INDEFINITE);
        tic.play();
        iniciarAudio();
        actualizar();
    }

    // ------------------------------------------------------------------ construcción

    /** Aplica el fondo guardado. Devuelve false si no se pudo y se quedó con el degradado del tema. */
    private boolean construirFondo() {
        velo.setStyle("-fx-background-color: black;");
        velo.setOpacity(st.integer("timeBlocking.backgroundDim", 0, 90) / 100.0);
        velo.setMouseTransparent(true);
        capaFondo.getChildren().clear();
        if (video != null) { video.dispose(); video = null; }

        String modo = st.choice("timeBlocking.background", "degradado", "archivo", "color");
        String ruta = st.str("timeBlocking.backgroundFile");
        String color = Colors.normalize(st.str("timeBlocking.backgroundColor"));
        boolean puesto = false;
        if (modo.equals("archivo") && !ruta.isBlank() && new File(ruta).isFile()) {
            puesto = ponerFondoArchivo(new File(ruta));
        } else if (modo.equals("color") && color != null) {
            capaFondo.setStyle("-fx-background-color: " + color + ";");
            puesto = true;
        }
        if (!puesto) capaFondo.setStyle("-fx-background-color: linear-gradient(to bottom right, -bg, -surface3);");
        raiz.getChildren().setAll(capaFondo, velo, capaContenido);
        return puesto;
    }

    /** Pone una imagen o un video de fondo. Devuelve false si el archivo no se pudo usar. */
    private boolean ponerFondoArchivo(File f) {
        String ext = extension(f);
        try {
            if (VIDEO.contains(ext)) {
                video = new MediaPlayer(new Media(f.toURI().toString()));
                video.setCycleCount(st.bool("timeBlocking.backgroundLoop") ? MediaPlayer.INDEFINITE : 1);
                video.setMute(true);
                MediaView mv = new MediaView(video);
                mv.fitWidthProperty().bind(capaFondo.widthProperty());
                mv.fitHeightProperty().bind(capaFondo.heightProperty());
                mv.setPreserveRatio(false);
                mv.setMouseTransparent(true);
                capaFondo.getChildren().setAll(mv);
                video.play();
                return true;
            }
            if (IMAGEN.contains(ext)) {
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.fitWidthProperty().bind(capaFondo.widthProperty());
                iv.fitHeightProperty().bind(capaFondo.heightProperty());
                iv.setPreserveRatio(false);
                iv.setMouseTransparent(true);
                capaFondo.getChildren().setAll(iv);
                return true;
            }
        } catch (RuntimeException ex) {
            if (video != null) { video.dispose(); video = null; }
        }
        return false;
    }

    private void construirContenido(String titulo, MetodoEnfoque metodo) {
        fase.getStyleClass().add("h2");
        reloj.getStyleClass().add("enfoque-reloj");
        reloj.setStyle("-fx-font-size: " + tamanoReloj() + "px;");
        detalle.getStyleClass().add("caption");
        detalle.setWrapText(true);
        detalle.setMaxWidth(560);
        detalle.setAlignment(Pos.CENTER);
        aviso.getStyleClass().add("caption");
        aviso.setWrapText(true);
        aviso.setMaxWidth(560);
        barra.setPrefHeight(14);
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.getStyleClass().add("enfoque-barra");

        tarea.setText(titulo == null || titulo.isBlank() ? metodo.nombre() : titulo);
        tarea.getStyleClass().add("h3");
        tarea.setWrapText(true);
        tarea.setMaxWidth(560);
        tarea.setAlignment(Pos.CENTER);
        tarea.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox controles = new HBox(10, play, reiniciar, siguiente, terminarFlow, detener);
        controles.setAlignment(Pos.CENTER);
        HBox extras = new HBox(10, botonFondo, botonAjustes);
        extras.setAlignment(Pos.CENTER);

        VBox cuerpo = new VBox(12, tarea, fase, reloj, barra, detalle, controles, extras, aviso);
        cuerpo.setAlignment(Pos.CENTER);
        cuerpo.setMaxWidth(640);
        VBox centro = new VBox(cuerpo);
        centro.setPadding(new Insets(30));
        centro.setAlignment(Pos.CENTER);
        centro.setMaxWidth(640);
        centro.getStyleClass().add("card");
        centro.getStyleClass().add("enfoque-centro");
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        VBox panelLista = panelPendientes();

        HBox fila = new HBox(20, centro);
        fila.setAlignment(Pos.CENTER);
        if (panelLista != null) fila.getChildren().add(panelLista);

        panelAjustes.setPadding(new Insets(20));
        panelAjustes.getStyleClass().addAll("card", "enfoque-ajustes");
        panelAjustes.setMaxWidth(360);
        panelAjustes.setVisible(false);
        panelAjustes.setManaged(false);
        panelAjustes.getChildren().addAll(construirAjustes());

        StackPane.setMargin(fila, new Insets(20));
        StackPane.setMargin(panelAjustes, new Insets(20));
        capaContenido.getChildren().setAll(fila, panelAjustes);
        StackPane.setAlignment(panelAjustes, Pos.TOP_RIGHT);
    }

    /** Panel lateral con los pendientes (tareas y/o casillas de las notas). */
    private VBox panelPendientes() {
        if (!st.bool("timeBlocking.showTasks") || pendientes.isEmpty()) return null;
        listaPendientes.getChildren().clear();
        Label h = new Label("Pendientes");
        h.getStyleClass().add("h3");
        listaPendientes.getChildren().add(h);
        int n = 0;
        for (ListaPendientes.Item it : pendientes) {
            if (n++ >= 12) break;
            CheckBox cb = new CheckBox(it.texto());
            cb.setSelected(it.hecho());
            cb.setWrapText(true);
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.getStyleClass().add("enfoque-pendiente");
            cb.setOnAction(e -> {
                boolean hecho = cb.isSelected();
                if (alAlternar != null) alAlternar.accept(it, hecho);
                if (hecho) cb.getStyleClass().add("tachado");
                else cb.getStyleClass().remove("tachado");
            });
            if (it.hecho()) cb.getStyleClass().add("tachado");
            listaPendientes.getChildren().add(cb);
        }
        scrollPendientes.setFitToWidth(true);
        scrollPendientes.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPendientes.setPrefWidth(300);
        scrollPendientes.setMaxWidth(300);
        scrollPendientes.setPrefHeight(320);
        VBox panel = new VBox(10, scrollPendientes);
        panel.getStyleClass().addAll("card", "enfoque-pendientes");
        panel.setPrefWidth(320);
        panel.setMaxWidth(320);
        panel.setPadding(new Insets(18));
        return panel;
    }

    // ------------------------------------------------------------------ panel de ajustes

    private Node[] construirAjustes() {
        Label t = Ui.h3("Ajustes de la pantalla");
        Button cerrar = Ui.iconButton("close", "Cerrar los ajustes");
        cerrar.setOnAction(e -> alternarAjustes());
        HBox head = new HBox(10, t, Ui.spacer(), cerrar);
        head.setAlignment(Pos.CENTER_LEFT);

        Ui.Segmented tam = new Ui.Segmented("Pequeño", "Mediano", "Grande");
        tam.select(Math.max(0, Math.min(2, indiceTamano())));
        tam.selectedProperty().addListener((o, a, v) -> {
            st.set("timeBlocking.clockSize", new String[]{"pequeño", "mediano", "grande"}[v.intValue()]);
            reloj.setStyle("-fx-font-size: " + tamanoReloj() + "px;");
        });

        Switch verBarra = new Switch("Mostrar la barra de progreso", null, st.bool("timeBlocking.showProgress"));
        verBarra.selectedProperty().addListener((o, a, v) -> { st.set("timeBlocking.showProgress", v); barra.setVisible(v); });

        Switch verDetalle = new Switch("Mostrar el detalle de la sesión", "Sesión, ciclo y método.", st.bool("timeBlocking.showDetail"));
        verDetalle.selectedProperty().addListener((o, a, v) -> { st.set("timeBlocking.showDetail", v); detalle.setVisible(v); });

        Switch verLista = new Switch("Mostrar la lista de pendientes", null, st.bool("timeBlocking.showTasks"));
        verLista.selectedProperty().addListener((o, a, v) -> { st.set("timeBlocking.showTasks", v); });

        Slider veloSlider = new Slider(0, 85, st.integer("timeBlocking.backgroundDim", 0, 90));
        veloSlider.setBlockIncrement(5);
        Label veloVal = Ui.label(veloSlider.getValue() + " %", "caption");
        veloSlider.valueProperty().addListener((o, a, v) -> {
            veloVal.setText(Math.round(v.doubleValue()) + " %");
            velo.setOpacity(v.doubleValue() / 100.0);
        });
        veloSlider.setOnMouseReleased(e -> st.set("timeBlocking.backgroundDim", (int) Math.round(veloSlider.getValue())));
        HBox veloFila = new HBox(10, veloSlider, veloVal);
        veloSlider.setPrefWidth(180);
        veloFila.setAlignment(Pos.CENTER_LEFT);

        Switch sonido = new Switch("Sonido al cambiar de fase", null, st.bool("timeBlocking.sound"));
        sonido.selectedProperty().addListener((o, a, v) -> st.set("timeBlocking.sound", v));

        // Audio local de la sesión (mp3, wav, ogg…): se repite mientras trabajas y calla en los descansos.
        Label audioNombre = Ui.label(nombreAudio(), "caption");
        Button elegirAudio = Ui.secondary("music", "Elegir audio…");
        elegirAudio.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Audio de la sesión de enfoque");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg",
                "*.m4a", "*.aac", "*.flac", "*.opus"));
            File f = fc.showOpenDialog(stage);
            if (f == null) return;
            st.set("timeBlocking.audioFile", f.getAbsolutePath());
            audioNombre.setText(f.getName());
            iniciarAudio();
        });
        Button quitarAudio = Ui.secondary(null, "Quitar");
        quitarAudio.setOnAction(e -> {
            st.set("timeBlocking.audioFile", null);
            audioNombre.setText("Sin audio");
            pararAudio();
        });
        HBox audioFila = new HBox(10, elegirAudio, audioNombre, quitarAudio);
        audioFila.setAlignment(Pos.CENTER_LEFT);

        Switch repetir = new Switch("Repetir el video de fondo", "Solo si el fondo es un video.", st.bool("timeBlocking.backgroundLoop"));
        repetir.selectedProperty().addListener((o, a, v) -> {
            st.set("timeBlocking.backgroundLoop", v);
            if (video != null) video.setCycleCount(v ? MediaPlayer.INDEFINITE : 1);
        });

        javafx.scene.control.ComboBox<Importancia> imp = new javafx.scene.control.ComboBox<>();
        imp.getItems().setAll(Importancia.values());
        imp.setValue(importancia);
        imp.setOnAction(e -> {
            Importancia n = imp.getValue();
            if (n == null) return;
            importancia = n;
            st.set("timeBlocking.importance", n.name());
            aplicarImportancia();
        });

        Button quitarFondo = Ui.secondary(null, "Quitar el fondo");
        quitarFondo.setOnAction(e -> {
            st.set("timeBlocking.backgroundFile", null);
            st.set("timeBlocking.background", "degradado");
            construirFondo();
        });

        // Un botón propio: si reutilizáramos el de la pantalla principal, se movería de sitio al
        // abrirse este panel y el botón original desaparecería de la fila de abajo.
        Button cambiarFondo = Ui.secondary("grid", "Cambiar el fondo");
        cambiarFondo.setOnAction(e -> elegirFondo());

        return new Node[]{
            head,
            Ui.muted("Los cambios se guardan en tu perfil y se aplican al instante."),
            Ui.field("Tamaño del reloj", tam.node()),
            verBarra, verDetalle, verLista,
            Ui.field("Velo sobre el fondo", veloFila),
            sonido,
            Ui.field("Audio de la sesión", audioFila),
            Ui.muted("Un archivo de audio de tu equipo: se repite mientras trabajas y calla en los descansos."),
            repetir,
            Ui.field("Importancia de la sesión", imp),
            Ui.muted(importancia.ayuda()),
            new HBox(10, cambiarFondo, quitarFondo)
        };
    }

    private int indiceTamano() {
        return switch (st.str("timeBlocking.clockSize")) {
            case "pequeño" -> 0;
            case "grande" -> 2;
            default -> 1;
        };
    }

    private int tamanoReloj() {
        return switch (indiceTamano()) {
            case 0 -> 56;
            case 2 -> 124;
            default -> 88;
        };
    }

    private void alternarAjustes() {
        ajustesAbiertos = !ajustesAbiertos;
        panelAjustes.setVisible(ajustesAbiertos);
        panelAjustes.setManaged(ajustesAbiertos);
        if (ajustesAbiertos) {
            panelAjustes.getChildren().setAll(construirAjustes());
            panelAjustes.requestFocus();
        }
    }

    // ------------------------------------------------------------------ fondo

    private void elegirFondo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Fondo de la pantalla de enfoque");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y videos", "*.png", "*.jpg", "*.jpeg",
            "*.gif", "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.webm", "*.ogv"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        String ext = extension(f);
        if (!VIDEO.contains(ext) && !IMAGEN.contains(ext)) {
            Ui.alert(stage.getScene().getRoot(), "No se pudo usar ese archivo",
                "Elige una imagen (png, jpg, gif, bmp, webp) o un video (mp4, webm).");
            return;
        }
        st.set("timeBlocking.backgroundFile", f.getAbsolutePath());
        st.set("timeBlocking.background", "archivo");
        if (!construirFondo()) {
            st.set("timeBlocking.backgroundFile", null);
            st.set("timeBlocking.background", "degradado");
            construirFondo();
            Ui.alert(stage.getScene().getRoot(), "No se pudo usar ese archivo",
                "El archivo no se pudo abrir: puede estar dañado o en un formato que JavaFX no reproduce.");
        }
    }

    /** Extensión en minúsculas, sin el punto ("png", "mp4"). */
    private static String extension(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.contains(".") ? n.substring(n.lastIndexOf('.') + 1) : "";
    }

    // ------------------------------------------------------------------ audio de la sesión

    /** Nombre del archivo de audio elegido (o "Sin audio"). */
    private String nombreAudio() {
        String ruta = st.str("timeBlocking.audioFile");
        return ruta.isBlank() ? "Sin audio" : new File(ruta).getName();
    }

    /** Prepara el audio local y lo deja listo (sin sonido hasta que empiece la fase de trabajo). */
    private void iniciarAudio() {
        pararAudio();
        String ruta = st.str("timeBlocking.audioFile");
        if (ruta.isBlank()) return;
        File f = new File(ruta);
        if (!f.isFile()) return;
        try {
            audio = new MediaPlayer(new Media(f.toURI().toString()));
            audio.setCycleCount(MediaPlayer.INDEFINITE);
            audio.setVolume(st.integer("timeBlocking.volume", 0, 100) / 100f);
            audio.setOnError(() -> { audio = null; });
        } catch (RuntimeException ex) {
            audio = null;
        }
    }

    private void pararAudio() {
        if (audio != null) { audio.dispose(); audio = null; }
    }

    /** El audio suena durante el trabajo y se calla en los descansos o si la sesión está en pausa. */
    private void sincronizarAudio() {
        if (audio == null) return;
        boolean debeSonar = !sesion.pausada() && !sesion.terminada() && sesion.fase() == SesionEnfoque.Fase.TRABAJO;
        try {
            if (debeSonar) {
                if (audio.getStatus() != MediaPlayer.Status.PLAYING) audio.play();
            } else if (audio.getStatus() == MediaPlayer.Status.PLAYING) {
                audio.pause();
            }
        } catch (RuntimeException ignored) { /* archivo de audio no disponible */ }
    }

    // ------------------------------------------------------------------ importancia

    private void aplicarImportancia() {
        stage.setAlwaysOnTop(false);
        stage.setFullScreen(false);
        if (importancia == Importancia.ESTRICTA) {
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
            // Dentro de la app no funcionan los atajos (Ctrl+S, F5…), salvo Escape para salir.
            if (!filtroTeclado) {
                filtroTeclado = true;
                stage.getScene().addEventFilter(KeyEvent.ANY, filtroEstricto);
            }
        } else {
            // Si se sale del modo Estricta, el filtro de teclado se retira (si no, la app se queda muda).
            if (filtroTeclado) {
                filtroTeclado = false;
                stage.getScene().removeEventFilter(KeyEvent.ANY, filtroEstricto);
            }
            if (importancia == Importancia.ALTA) stage.setMaximized(true);
        }
    }

    /** Filtro del modo Estricta: se come los atajos y deja Escape para poder salir con confirmación. */
    private final javafx.event.EventHandler<KeyEvent> filtroEstricto = e -> {
        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            e.consume();
            cerrar(true);
        } else e.consume();
    };

    // ------------------------------------------------------------------ ciclo

    private void alternar() {
        if (sesion.pausada()) { sesion.iniciar(); aviso.setText(""); } else sesion.pausar();
        actualizar();
    }

    private void salio() {
        if (dialogoAbierto || ajustesAbiertos) return;
        if (importancia == Importancia.NORMAL && !st.bool("timeBlocking.pauseOnLeave")) return;
        if (sesion.pausada() || sesion.terminada()) return;
        sesion.pausar();
        salidas++;
        aviso.setText("El contador se pausó porque saliste de la pantalla (" + salidas
            + (salidas == 1 ? " vez" : " veces") + "). Pulsa «Continuar» cuando vuelvas.");
        actualizar();
        if (importancia == Importancia.ESTRICTA) { stage.setIconified(false); stage.toFront(); stage.requestFocus(); }
    }

    private void beep() {
        try {
            float v = st.integer("timeBlocking.volume", 0, 100) / 100f;
            if (v <= 0) return;
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) { /* sin audio disponible */ }
    }

    private void avisarFinDeFase() {
        if (!st.bool("timeBlocking.notifyEnd")) return;
        String msg = switch (sesion.fase()) {
            case TRABAJO -> "¡Descanso! Estira las espalda.";
            case DESCANSO, DESCANSO_LARGO -> "Vuelve al trabajo.";
        };
        aviso.setText(msg);
    }

    private void actualizar() {
        MetodoEnfoque m = sesion.metodo();
        String nombreFase = switch (sesion.fase()) {
            case TRABAJO -> "Trabajo";
            case DESCANSO -> "Descanso";
            case DESCANSO_LARGO -> "Descanso largo";
        };
        fase.setText(nombreFase);
        barra.getStyleClass().removeAll("fase-trabajo", "fase-descanso", "fase-largo");
        barra.getStyleClass().add(sesion.fase() == SesionEnfoque.Fase.TRABAJO ? "fase-trabajo"
            : sesion.fase() == SesionEnfoque.Fase.DESCANSO ? "fase-descanso" : "fase-largo");
        reloj.setText(SesionEnfoque.formato(sesion.restantes()));
        barra.setProgress(sesion.cuentaLibre() ? ProgressBar.INDETERMINATE_PROGRESS : sesion.progreso());
        barra.setVisible(st.bool("timeBlocking.showProgress"));

        String ses = m.sesionesPorCiclo() > 0
            ? "Sesión " + sesion.sesion() + " de " + m.sesionesPorCiclo() + " · Ciclo " + sesion.ciclo()
            : "Sesión " + sesion.sesion();
        String duracion = m.tieneRangos()
            ? " · ahora " + sesion.trabajoUsado() + " min de trabajo y " + sesion.descansoUsado() + " de descanso"
            : "";
        detalle.setText(ses + " · " + m.nombre() + " · " + m.resumen() + duracion);
        detalle.setVisible(st.bool("timeBlocking.showDetail"));

        play.setText(sesion.pausada()
            ? (sesion.transcurrido() == 0 && sesion.sesion() == 1 && sesion.ciclo() == 1 ? "Iniciar" : "Continuar")
            : "Pausar");
        terminarFlow.setVisible(sesion.cuentaLibre());
        terminarFlow.setManaged(sesion.cuentaLibre());
        sincronizarAudio();
    }

    private void cerrar(boolean confirmar) {
        if (confirmar && !sesion.terminada() && sesion.transcurrido() > 0
            && !Ui.confirm(stage.getScene().getRoot(), "¿Detener la sesión de enfoque?", "Se pierde el avance de la fase actual.", "Detener")) return;
        sesion.detener();
        if (tic != null) tic.stop();
        if (video != null) { video.dispose(); video = null; }
        pararAudio();
        stage.setAlwaysOnTop(false);
        stage.close();
    }
}
