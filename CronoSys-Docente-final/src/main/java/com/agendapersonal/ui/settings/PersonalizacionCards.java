package com.agendapersonal.ui.settings;

import com.agendapersonal.settings.Colors;
import com.agendapersonal.settings.Palette;
import com.agendapersonal.settings.SettingsStore;
import com.agendapersonal.settings.ThemePreset;
import com.agendapersonal.ui.FontPreferences;
import com.agendapersonal.ui.Notifier;
import com.agendapersonal.ui.Switch;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tarjetas de personalización de la pantalla Configuración: temas, crear tema, forma y texto,
 * perfiles, y respaldo (exportar / importar / restablecer). Todo se aplica al instante.
 */
public final class PersonalizacionCards {
    private PersonalizacionCards() {}

    private static final String[] EXTRA_ACCENTS = {"#1F9D9A", "#D9A400", "#4C5BD4", "#9A6A3A"};
    private static final String[] EXTRA_NAMES = {"Turquesa", "Amarillo", "Índigo", "Marrón"};

    private static String hex(Color c) {
        return Colors.hex((int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    // ---------------------------------------------------------------------------- temas

    public static VBox temas() {
        ThemeService theme = ThemeService.get();
        FlowPane flow = new FlowPane(12, 12);
        List<Button> botones = new ArrayList<>();
        for (ThemePreset p : ThemePreset.ALL) {
            Region fondo = new Region();
            fondo.setPrefSize(104, 56);
            fondo.setMinSize(104, 56);
            Region tarjeta = new Region();
            tarjeta.setPrefSize(60, 18);
            tarjeta.setMaxSize(60, 18);
            tarjeta.setStyle("-fx-background-color:" + p.card() + "; -fx-background-radius:6;");
            Region punto = new Region();
            punto.setPrefSize(14, 14);
            punto.setMaxSize(14, 14);
            punto.setStyle("-fx-background-color:" + p.accent() + "; -fx-background-radius:99;");
            Region texto = new Region();
            texto.setPrefSize(34, 5);
            texto.setMaxSize(34, 5);
            texto.setStyle("-fx-background-color:" + p.text() + "; -fx-background-radius:3;");
            VBox dentro = new VBox(6, texto, new HBox(6, tarjeta, punto));
            dentro.setAlignment(Pos.CENTER_LEFT);
            dentro.setPadding(new javafx.geometry.Insets(10));
            fondo.setStyle("-fx-background-color:" + p.bg() + "; -fx-background-radius:10;");
            javafx.scene.layout.StackPane vista = new javafx.scene.layout.StackPane(fondo, dentro);
            vista.setMaxSize(104, 56);
            Label nombre = new Label(p.name());
            nombre.getStyleClass().add("caption");
            Button b = new Button();
            b.setGraphic(new VBox(6, vista, nombre));
            b.getStyleClass().add("theme-tile");
            b.setAccessibleText("Tema " + p.name());
            b.getProperties().put("theme", p.id());
            b.setOnAction(e -> theme.setTheme(p.id()));
            Ui.tooltip(b, p.name());
            botones.add(b);
            flow.getChildren().add(b);
        }
        Runnable marcar = () -> {
            String actual = theme.settings().str("appearance.themeId");
            for (Button b : botones) b.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), b.getProperties().get("theme").equals(actual));
        };
        marcar.run();
        theme.revisionProperty().addListener((o, a, b) -> marcar.run());
        VBox card = Ui.card();
        card.getChildren().addAll(Ui.h2("Temas"),
            Ui.muted("Elige una combinación de colores lista. Puedes ajustarla abajo con «Crear tema»."), flow);
        return card;
    }

    // ---------------------------------------------------------------------------- crear tema

    public static VBox crearTema() {
        ThemeService theme = ThemeService.get();
        SettingsStore st = theme.settings();
        boolean[] interno = {false};

        ColorPicker bg = new ColorPicker(), card = new ColorPicker(), text = new ColorPicker(), accent = new ColorPicker();
        Label aviso = new Label();
        aviso.getStyleClass().add("muted");
        aviso.setWrapText(true);
        TextField hexField = new TextField();
        hexField.setPromptText("#RRGGBB");
        hexField.setPrefColumnCount(8);

        Runnable sync = () -> {
            interno[0] = true;
            Palette.Resolved r = theme.resolved();
            bg.setValue(Color.web(theme.color("-bg")));
            card.setValue(Color.web(theme.color("-surface")));
            text.setValue(Color.web(theme.color("-text")));
            Color ac = Color.web(theme.color("-accent"));
            accent.setValue(ac);
            hexField.setText(hex(ac));
            interno[0] = false;
            List<String> w = Palette.contrastWarnings(theme.color("-bg"), theme.color("-surface"), theme.color("-text"), theme.color("-accent"));
            aviso.setText(w.isEmpty() ? "" : "Aviso: " + String.join(" ", w) + " Puedes dejarlo así, es solo una sugerencia.");
            aviso.setVisible(!w.isEmpty());
            aviso.setManaged(!w.isEmpty());
        };
        sync.run();
        theme.revisionProperty().addListener((o, a, b) -> sync.run());

        bg.setOnAction(e -> { if (!interno[0]) st.set("appearance.colors.bg", hex(bg.getValue())); });
        card.setOnAction(e -> { if (!interno[0]) st.set("appearance.colors.card", hex(card.getValue())); });
        text.setOnAction(e -> { if (!interno[0]) st.set("appearance.colors.text", hex(text.getValue())); });
        accent.setOnAction(e -> { if (!interno[0]) st.set("appearance.colors.accent", hex(accent.getValue())); });
        Runnable aplicarHex = () -> {
            String h = Colors.normalize(hexField.getText());
            if (h == null) { hexField.setText(hex(accent.getValue())); return; }
            st.set("appearance.colors.accent", h);
        };
        hexField.setOnAction(e -> aplicarHex.run());
        hexField.focusedProperty().addListener((o, a, f) -> { if (!f) aplicarHex.run(); });

        FlowPane extras = new FlowPane(10, 10);
        for (int i = 0; i < EXTRA_ACCENTS.length; i++) {
            final String h = EXTRA_ACCENTS[i];
            Button b = new Button();
            b.setGraphic(new Region());
            ((Region) b.getGraphic()).setPrefSize(26, 26);
            ((Region) b.getGraphic()).setStyle("-fx-background-color:" + h + "; -fx-background-radius:99;");
            b.getStyleClass().add("swatch-button");
            b.setAccessibleText("Acento " + EXTRA_NAMES[i]);
            Ui.tooltip(b, EXTRA_NAMES[i]);
            b.setOnAction(e -> st.set("appearance.colors.accent", h));
            extras.getChildren().add(b);
        }

        Button quitar = Ui.secondary(null, "Quitar personalización");
        quitar.setOnAction(e -> theme.setTheme(st.str("appearance.themeId")));

        HBox colores = new HBox(14, Ui.field("Fondo", bg), Ui.field("Tarjetas", card), Ui.field("Texto", text), Ui.field("Acento", accent));
        HBox hexFila = new HBox(10, hexField, extras);
        hexFila.setAlignment(Pos.CENTER_LEFT);
        VBox box = Ui.card();
        box.getChildren().addAll(Ui.h2("Crear tema"),
            Ui.muted("Cambia los cuatro colores base; el resto de la interfaz se calcula solo. El texto sobre el acento se elige automáticamente."),
            colores, Ui.field("Acento en hexadecimal o de la paleta extra", hexFila), aviso, quitar);
        return box;
    }

    // ---------------------------------------------------------------------------- forma y texto

    public static VBox forma() {
        ThemeService theme = ThemeService.get();
        SettingsStore st = theme.settings();
        boolean[] interno = {false};

        Slider escala = new Slider(85, 125, FontPreferences.scalePercent());
        escala.setMajorTickUnit(5);
        escala.setBlockIncrement(1);
        Label escalaVal = new Label(FontPreferences.scalePercent() + " %");
        escala.valueProperty().addListener((o, a, v) -> escalaVal.setText(Math.round(v.doubleValue()) + " %"));
        Runnable aplicarEscala = () -> { if (!interno[0]) FontPreferences.setScale((int) Math.round(escala.getValue())); };
        escala.valueChangingProperty().addListener((o, a, v) -> { if (!v) aplicarEscala.run(); });
        escala.valueProperty().addListener((o, a, v) -> { if (!escala.isValueChanging()) aplicarEscala.run(); });

        Slider radio = new Slider(0, 28, theme.radius());
        radio.setBlockIncrement(1);
        Label radioVal = new Label(theme.radius() + " px");
        radio.valueProperty().addListener((o, a, v) -> radioVal.setText(Math.round(v.doubleValue()) + " px"));
        Runnable aplicarRadio = () -> { if (!interno[0]) st.set("appearance.radius", (int) Math.round(radio.getValue())); };
        radio.valueChangingProperty().addListener((o, a, v) -> { if (!v) aplicarRadio.run(); });
        radio.valueProperty().addListener((o, a, v) -> { if (!radio.isValueChanging()) aplicarRadio.run(); });

        String[] dens = {"compacta", "comoda", "amplia"};
        Ui.Segmented densidad = new Ui.Segmented("Compacta", "Cómoda", "Amplia");
        densidad.select(java.util.Arrays.asList(dens).indexOf(st.choice("appearance.density", dens)));
        densidad.selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.set("appearance.density", dens[v.intValue()]); });

        Switch calido = new Switch("Bajo contraste y luz cálida", "Filtro suave para cuidar la vista.", st.bool("appearance.warmLowContrast"));
        calido.selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.set("appearance.warmLowContrast", v); });
        Switch animaciones = new Switch("Reducir animaciones", "Quita los movimientos y transiciones.", st.bool("appearance.reduceMotion"));
        animaciones.selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.set("appearance.reduceMotion", v); });
        Switch porHora = new Switch("Tema automático por hora", "Claro de día y oscuro de noche (19:00 a 07:00), con los temas Claro/Oscuro.",
            st.bool("appearance.autoThemeByTime"));
        porHora.selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.set("appearance.autoThemeByTime", v); });

        // Si cambia el perfil, se leen los valores del nuevo (sin volver a escribirlos).
        theme.revisionProperty().addListener((o, a, b) -> {
            interno[0] = true;
            escala.setValue(FontPreferences.scalePercent());
            radio.setValue(theme.radius());
            densidad.select(java.util.Arrays.asList(dens).indexOf(st.choice("appearance.density", dens)));
            calido.selectedProperty().set(st.bool("appearance.warmLowContrast"));
            animaciones.selectedProperty().set(st.bool("appearance.reduceMotion"));
            porHora.selectedProperty().set(st.bool("appearance.autoThemeByTime"));
            interno[0] = false;
        });

        HBox escalaFila = new HBox(12, escala, escalaVal);
        HBox radioFila = new HBox(12, radio, radioVal);
        HBox.setHgrow(escala, Priority.ALWAYS);
        HBox.setHgrow(radio, Priority.ALWAYS);
        escalaFila.setAlignment(Pos.CENTER_LEFT);
        radioFila.setAlignment(Pos.CENTER_LEFT);
        VBox box = Ui.card();
        box.getChildren().addAll(Ui.h2("Forma, tamaño y efectos"),
            Ui.field("Escala del texto (85 % a 125 %)", escalaFila),
            Ui.field("Radio de las esquinas (0 a 28 px)", radioFila),
            Ui.field("Densidad", densidad.node()),
            sep(), calido, sep(), animaciones, sep(), porHora);
        return box;
    }

    // ---------------------------------------------------------------------------- perfiles

    public static VBox perfiles() {
        SettingsStore st = ThemeService.get().settings();
        ComboBox<String> combo = new ComboBox<>();
        boolean[] interno = {false};

        // ---- horario: varias franjas y días de la semana ----
        VBox listaFranjas = new VBox(8);
        List<TextField> camposDesde = new ArrayList<>(), camposHasta = new ArrayList<>();
        List<javafx.scene.control.ToggleButton> botonesDia = new ArrayList<>();
        String[] etiquetasDia = {"L", "M", "X", "J", "V", "S", "D"};
        // Lo que se está editando todavía no está guardado: se guarda al pulsar «Guardar horario».
        final List<SettingsStore.Franja> borrador = new ArrayList<>();
        final Runnable[] repintar = new Runnable[1];   // referencia al propio repintado (para el botón de borrar)

        Runnable pintarFranjas = () -> {
            camposDesde.clear(); camposHasta.clear();
            listaFranjas.getChildren().clear();
            for (int i = 0; i < borrador.size(); i++) {
                final int idx = i;
                TextField desde = new TextField(borrador.get(i).desde());
                TextField hasta = new TextField(borrador.get(i).hasta());
                desde.setPrefColumnCount(5); hasta.setPrefColumnCount(5);
                desde.textProperty().addListener((o, a, b) ->
                    borrador.set(idx, new SettingsStore.Franja(b, hasta.getText())));
                hasta.textProperty().addListener((o, a, b) ->
                    borrador.set(idx, new SettingsStore.Franja(desde.getText(), b)));
                Button quitar = Ui.iconButton("delete", "Quitar esta franja");
                quitar.setOnAction(e -> { borrador.remove(idx); repintar[0].run(); });
                HBox fila = new HBox(8, desde, new Label("a"), hasta, quitar);
                fila.setAlignment(Pos.CENTER_LEFT);
                listaFranjas.getChildren().add(fila);
                camposDesde.add(desde); camposHasta.add(hasta);
            }
        };
        repintar[0] = pintarFranjas;

        Runnable cargarFranjas = () -> {
            borrador.clear();
            borrador.addAll(st.franjas(st.activeProfileId()));
            if (borrador.isEmpty()) borrador.add(new SettingsStore.Franja("08:00", "14:00"));
            pintarFranjas.run();
            List<Integer> dias = new ArrayList<>(st.dias(st.activeProfileId()));
            for (int i = 0; i < botonesDia.size(); i++) botonesDia.get(i).setSelected(dias.contains(i + 1));
        };

        Runnable cargar = () -> {
            interno[0] = true;
            List<String> nombres = new ArrayList<>();
            for (Map<String, Object> p : st.profiles()) nombres.add(p.get("id") + " · " + p.get("name"));
            combo.getItems().setAll(nombres);
            combo.getSelectionModel().select(nombres.indexOf(st.activeProfileId() + " · " + st.activeProfileName()));
            interno[0] = false;
            cargarFranjas.run();
        };
        cargar.run();
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? null : s.substring(s.indexOf('·') + 2)); }
        });
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? null : s.substring(s.indexOf('·') + 2)); }
        });
        combo.setOnAction(e -> {
            if (interno[0] || combo.getValue() == null) return;
            st.activateProfile(combo.getValue().substring(0, combo.getValue().indexOf(' ')));
            cargarFranjas.run();
        });

        Button nuevo = Ui.secondary(null, "Nuevo"), renombrar = Ui.secondary(null, "Renombrar"),
               duplicar = Ui.secondary(null, "Duplicar"), borrar = Ui.secondary(null, "Eliminar");
        nuevo.setOnAction(e -> pedirNombre("Nuevo perfil", "Perfil nuevo", n -> { String id = st.createProfile(n); cargar.run(); st.activateProfile(id); }));
        renombrar.setOnAction(e -> pedirNombre("Renombrar perfil", st.activeProfileName(), n -> { st.renameProfile(st.activeProfileId(), n); cargar.run(); }));
        duplicar.setOnAction(e -> { String id = st.duplicateProfile(st.activeProfileId()); cargar.run(); st.activateProfile(id); });
        borrar.setOnAction(e -> {
            if (st.profiles().size() <= 1) { Ui.alert(borrar, "No se puede eliminar", "Siempre debe quedar al menos un perfil."); return; }
            if (Ui.confirm(borrar, "¿Eliminar el perfil «" + st.activeProfileName() + "»?", "Se borran sus ajustes de personalización. Tus datos (eventos, tareas, notas…) no se tocan.", "Eliminar")) {
                st.deleteProfile(st.activeProfileId());
                cargar.run();
            }
        });

        // Días de la semana
        HBox filaDias = new HBox(6);
        for (int i = 0; i < etiquetasDia.length; i++) {
            javafx.scene.control.ToggleButton b = new javafx.scene.control.ToggleButton(etiquetasDia[i]);
            b.getStyleClass().add("segment");
            b.setMnemonicParsing(false);
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setAccessibleText("Día " + etiquetasDia[i]);
            botonesDia.add(b);
            filaDias.getChildren().add(b);
        }
        filaDias.getStyleClass().add("segmented");
        filaDias.setAlignment(Pos.CENTER_LEFT);

        Button anadir = Ui.secondary("add", "Añadir franja");
        anadir.setOnAction(e -> {
            borrador.add(new SettingsStore.Franja("16:00", "18:00"));
            pintarFranjas.run();
        });

        Button aplicarHorario = Ui.primary("check", "Guardar horario"), sinHorario = Ui.secondary(null, "Sin horario");
        aplicarHorario.setOnAction(e -> {
            List<SettingsStore.Franja> franjas = new ArrayList<>();
            for (int i = 0; i < camposDesde.size(); i++) {
                String a = camposDesde.get(i).getText().trim(), b = camposHasta.get(i).getText().trim();
                if (!a.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !b.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                    Ui.alert(aplicarHorario, "Horario inválido", "Escribe las horas como HH:mm, por ejemplo 08:00 y 14:00.");
                    return;
                }
                franjas.add(new SettingsStore.Franja(a, b));
            }
            List<Integer> dias = new ArrayList<>();
            for (int i = 0; i < botonesDia.size(); i++) if (botonesDia.get(i).isSelected()) dias.add(i + 1);
            st.setScheduleWindows(st.activeProfileId(), franjas, dias);
            Notifier.success("Horario guardado",
                dias.isEmpty() ? "El perfil se activará solo en esas franjas, todos los días."
                    : "El perfil se activará en esas franjas, los días marcados.");
            cargarFranjas.run();
        });
        sinHorario.setOnAction(e -> {
            st.setScheduleWindows(st.activeProfileId(), List.of(), List.of());
            cargarFranjas.run();
        });

        HBox fila1 = new HBox(8, nuevo, renombrar, duplicar, borrar);
        HBox filaBotones = new HBox(8, aplicarHorario, sinHorario, anadir);
        filaBotones.setAlignment(Pos.CENTER_LEFT);
        Label aviso = Ui.caption("Si no marcas ningún día, el perfil se activa esos días todos.");
        VBox box = Ui.card();
        box.getChildren().addAll(Ui.h2("Perfiles"),
            Ui.muted("Un perfil guarda todo tu estilo (tema, tamaños, opciones de cada ventana). Puede activarse "
                + "solo por horario: varias franjas y los días que tú elijas, por ejemplo Estudio de 08:00 a 12:00 "
                + "y de 16:00 a 18:00 solo de lunes a viernes."),
            Ui.field("Perfil activo", combo), fila1,
            Ui.field("Franjas horarias", listaFranjas),
            Ui.field("Días de la semana", filaDias),
            filaBotones, aviso);
        return box;
    }

    private static void pedirNombre(String titulo, String inicial, java.util.function.Consumer<String> ok) {
        TextInputDialog d = new TextInputDialog(inicial);
        d.setTitle("Compás");
        d.setHeaderText(titulo);
        d.setContentText("Nombre:");
        d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(ok);
    }

    // ---------------------------------------------------------------------------- silencio

    /** Horas de silencio: en ese rango no salen avisos de escritorio. */
    public static VBox silencio() {
        SettingsStore st = ThemeService.get().settings();
        TextField desde = new TextField(st.str("notifications.quietHours.from")), hasta = new TextField(st.str("notifications.quietHours.to"));
        desde.setPromptText("22:00");
        hasta.setPromptText("07:00");
        desde.setPrefColumnCount(5);
        hasta.setPrefColumnCount(5);
        Button guardar = Ui.secondary(null, "Guardar"), quitar = Ui.secondary(null, "Sin silencio");
        guardar.setOnAction(e -> {
            String a = desde.getText().trim(), b = hasta.getText().trim();
            if (!a.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !b.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                Ui.alert(guardar, "Horas inválidas", "Escribe las horas como HH:mm, por ejemplo 22:00 y 07:00.");
                return;
            }
            st.set("notifications.quietHours.from", a);
            st.set("notifications.quietHours.to", b);
        });
        quitar.setOnAction(e -> { st.set("notifications.quietHours.from", ""); st.set("notifications.quietHours.to", ""); desde.clear(); hasta.clear(); });
        HBox fila = new HBox(8, desde, new Label("a"), hasta, guardar, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox box = Ui.card();
        box.getChildren().addAll(Ui.h2("Horas de silencio"),
            Ui.muted("Entre estas horas no aparecen avisos de escritorio (por ejemplo de noche). Los avisos solo recuerdan eventos, cumpleaños y pendientes; guardar cambios no genera avisos salvo que lo actives en «Confirmaciones»."),
            fila);
        return box;
    }

    // ---------------------------------------------------------------------------- respaldo

    public static VBox respaldo() {
        SettingsStore st = ThemeService.get().settings();
        Button expActivo = Ui.secondary(null, "Exportar perfil activo"), expTodos = Ui.secondary(null, "Exportar todos"),
               imp = Ui.secondary(null, "Importar…"), reset = Ui.secondary(null, "Restablecer perfil activo");
        expActivo.setOnAction(e -> exportar(expActivo, st, false));
        expTodos.setOnAction(e -> exportar(expTodos, st, true));
        imp.setOnAction(e -> importar(imp, st));
        reset.setOnAction(e -> {
            if (Ui.confirm(reset, "¿Restablecer toda la personalización de este perfil?",
                "Vuelven los valores por defecto (tema, tamaños y opciones de cada ventana). Tus datos no se tocan.", "Restablecer")) {
                st.resetActiveProfile();
                FontPreferences.reload();
            }
        });
        FlowPane botones = new FlowPane(8, 8, expActivo, expTodos, imp, reset);
        VBox box = Ui.card();
        box.getChildren().addAll(Ui.h2("Respaldo de personalización"),
            Ui.muted("Exporta tu estilo a un archivo JSON para llevarlo a otra instalación. Al importar verás un resumen y deberás confirmar."), botones);
        return box;
    }

    private static void exportar(Node owner, SettingsStore st, boolean todos) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar personalización");
        fc.setInitialFileName(todos ? "compas-perfiles.json" : "compas-" + st.activeProfileName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(owner.getScene() == null ? null : owner.getScene().getWindow());
        if (f == null) return;
        try {
            Files.writeString(f.toPath(), st.export(todos), StandardCharsets.UTF_8);
            Notifier.success("Exportado", f.getName());
        } catch (Exception ex) {
            Ui.alert(owner, "No se pudo exportar", "No se pudo escribir el archivo: " + ex.getMessage());
        }
    }

    private static void importar(Node owner, SettingsStore st) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar personalización");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showOpenDialog(owner.getScene() == null ? null : owner.getScene().getWindow());
        if (f == null) return;
        String texto;
        try { texto = Files.readString(f.toPath(), StandardCharsets.UTF_8); }
        catch (Exception ex) { Ui.alert(owner, "No se pudo leer", "No se pudo abrir el archivo."); return; }
        SettingsStore.ImportPreview pv = SettingsStore.previewImport(texto);
        if (!pv.valid()) { Ui.alert(owner, "Archivo no válido", pv.message() + " No se cambió nada."); return; }
        if (Ui.confirm(owner, "¿Importar este archivo?", pv.message() + " Se activará el primero. Tus perfiles actuales se conservan.", "Importar")) {
            st.applyImport(pv);
            FontPreferences.reload();
            Notifier.success("Importado", "Personalización aplicada.");
        }
    }

    private static Region sep() {
        Region r = new Region();
        r.getStyleClass().add("hairline");
        return r;
    }
}
