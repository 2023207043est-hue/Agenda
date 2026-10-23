package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.domain.model.ActividadClase;
import com.agendapersonal.domain.model.ClaseDocente;
import com.agendapersonal.domain.model.EstudianteClase;
import com.agendapersonal.model.Evento;
import com.agendapersonal.model.Tarea;
import com.agendapersonal.infrastructure.repo.ClaseDocenteDAO;
import com.agendapersonal.service.ExcelClaseService;
import com.agendapersonal.service.MetodoEnfoque;
import com.agendapersonal.ui.EventViews;
import com.agendapersonal.ui.TaskViews;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.util.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Espacio profesional docente organizado en pestañas temáticas y conectado a la agenda local. */
public final class EspacioDocenteView {
    private final MainController controller;
    private final Stage stage;
    private final VBox root = Ui.page();
    private final TabPane tabs = new TabPane();
    private final Runnable abrirBloques;
    private final Runnable abrirCalendario;
    private final ClaseDocenteDAO clasesDao = new ClaseDocenteDAO();
    private Tab clasesTab;

    public EspacioDocenteView(MainController controller, Stage stage, Runnable abrirBloques, Runnable abrirCalendario) {
        this.controller = controller;
        this.stage = stage;
        this.abrirBloques = abrirBloques;
        this.abrirCalendario = abrirCalendario;
        Button planificar = Ui.secondary("calendar", "Abrir Time Blocking");
        planificar.setOnAction(e -> abrirBloques.run());
        root.getChildren().add(Ui.pageHeader("book", "Espacio Docente",
            "Prepara tus cursos, administra estudiantes y notas, organiza tutorías y reserva tiempo de concentración.", planificar));

        List<ClaseDocente> clases = clasesDao.listarClases();
        if (!clases.isEmpty()) root.getChildren().add(resumenDocente(clases));

        clasesTab = tab("Clases", "calendar", panelClases());
        tabs.getTabs().setAll(clasesTab, tab("Evaluación", "tasks", panelEvaluacion()),
            tab("Tutorías", "contacts", panelTutorias()), tab("Enfoque", "clock", panelEnfoque()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("teacher-tabs");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.getChildren().add(tabs);
    }

    public VBox getView() { return root; }

    private Tab tab(String title, String icon, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(com.agendapersonal.ui.IconFactory.icon(icon, 16));
        return tab;
    }

    private VBox panelClases() {
        VBox panel = section("Tus clases", "Cada curso tiene su propio resumen, planeación, lista de estudiantes y libro de calificaciones.");
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button add = Ui.primary("plus", "Crear clase");
        add.setOnAction(e -> crearClase());
        Label helper = Ui.muted("Los datos se guardan localmente y puedes intercambiar estudiantes y notas con Excel (.xlsx).");
        helper.setWrapText(true);
        toolbar.getChildren().addAll(add, helper);
        VBox.setVgrow(helper, Priority.NEVER);
        panel.getChildren().add(toolbar);

        List<ClaseDocente> clases = clasesDao.listarClases();
        if (clases.isEmpty()) {
            VBox empty = Ui.card();
            empty.getChildren().addAll(Ui.h3("Empieza creando tu primer curso"),
                Ui.muted("Después podrás abrir sus pestañas de resumen, planeación y estudiantes.") );
            panel.getChildren().add(empty);
            return panel;
        }
        TabPane cursos = new TabPane();
        cursos.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (ClaseDocente clase : clases) {
            Tab courseTab = new Tab(clase.toString(), panelCurso(clase));
            courseTab.setTooltip(new Tooltip(clase.periodo() == null ? clase.nombre() : clase.nombre() + " · " + clase.periodo()));
            cursos.getTabs().add(courseTab);
        }
        VBox.setVgrow(cursos, Priority.ALWAYS);
        panel.getChildren().add(cursos);
        return panel;
    }

    private Node panelCurso(ClaseDocente clase) {
        TabPane vistas = new TabPane();
        vistas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        vistas.getTabs().add(tab("Resumen", "book", resumenCurso(clase)));
        vistas.getTabs().add(tab("Planeación", "calendar", planeacionCurso(clase)));
        vistas.getTabs().add(tab("Estudiantes y notas", "contacts", estudiantesCurso(clase)));
        return vistas;
    }

    private VBox resumenCurso(ClaseDocente clase) {
        List<EstudianteClase> estudiantes = clasesDao.listarEstudiantes(clase.id());
        long conNota = estudiantes.stream().filter(s -> s.calificacion() != null).count();
        double promedio = estudiantes.stream().filter(s -> s.calificacion() != null)
            .mapToDouble(EstudianteClase::calificacion).average().orElse(Double.NaN);
        VBox box = section(clase.nombre(), "Panel de ayuda para " + (clase.grupo() == null ? "tu curso" : "el grupo " + clase.grupo())
            + (clase.periodo() == null ? "" : " · " + clase.periodo()) + ".");
        HBox cards = new HBox(12);
        cards.getChildren().addAll(metrica("Estudiantes", Integer.toString(estudiantes.size())),
            metrica("Con calificación", conNota + " / " + estudiantes.size()),
            metrica("Promedio del curso", Double.isNaN(promedio) ? "Sin notas" : String.format(java.util.Locale.forLanguageTag("es-CO"), "%.2f", promedio)));
        box.getChildren().add(cards);
        VBox ayuda = Ui.card();
        ayuda.getChildren().addAll(Ui.h3("Accesos rápidos"), Ui.muted("Mantén aquí el trabajo específico de este curso."));
        Button addActividad = Ui.secondary("plus", "Planear una actividad");
        addActividad.setOnAction(e -> { crearActividad(clase); clasesTab.setContent(panelClases()); });
        Button verEstudiantes = Ui.secondary("contacts", "Añadir estudiantes o usar Excel");
        verEstudiantes.setOnAction(e -> { /* El panel de estudiantes está disponible en la siguiente pestaña del curso. */
            mostrarInfo("Abre la pestaña «Estudiantes y notas» de este curso para gestionar la lista y el Excel."); });
        ayuda.getChildren().addAll(addActividad, verEstudiantes);
        box.getChildren().add(ayuda);
        return box;
    }

    private VBox metrica(String titulo, String valor) {
        VBox card = Ui.card();
        Label number = Ui.h2(valor);
        card.getChildren().addAll(Ui.muted(titulo), number);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Resumen general del docente: cuántos cursos maneja, estudiantes, promedio y evaluaciones abiertas. */
    private VBox resumenDocente(List<ClaseDocente> clases) {
        int estudiantes = 0;
        int conNota = 0;
        double suma = 0;
        for (ClaseDocente cl : clases) {
            List<EstudianteClase> est = clasesDao.listarEstudiantes(cl.id());
            estudiantes += est.size();
            for (EstudianteClase e : est) {
                if (e.calificacion() != null) { conNota++; suma += e.calificacion(); }
            }
        }
        double promedio = conNota == 0 ? Double.NaN : suma / conNota;
        long evaluacionesAbiertas = controller.getTareas().stream().filter(t -> !t.estaCompletada()).count();

        VBox strip = new VBox(12);
        HBox cards = new HBox(12);
        cards.getChildren().addAll(metrica("Cursos", Integer.toString(clases.size())),
            metrica("Estudiantes", Integer.toString(estudiantes)),
            metrica("Promedio del curso", Double.isNaN(promedio) ? "Sin notas" : String.format(java.util.Locale.forLanguageTag("es-CO"), "%.2f", promedio)),
            metrica("Evaluaciones abiertas", Long.toString(evaluacionesAbiertas)));
        strip.getChildren().add(cards);
        return strip;
    }

    private VBox planeacionCurso(ClaseDocente clase) {
        VBox box = section("Planeación de " + clase.nombre(), "Programa sesiones, materiales, actividades y evaluaciones propias de este curso.");
        Button add = Ui.primary("plus", "Añadir a la planeación");
        add.setOnAction(e -> { crearActividad(clase); clasesTab.setContent(panelClases()); });
        box.getChildren().add(add);
        List<ActividadClase> actividades = clasesDao.listarActividades(clase.id());
        if (actividades.isEmpty()) box.getChildren().add(Ui.muted("Aún no hay elementos. Añade una clase, actividad, material o evaluación."));
        for (ActividadClase actividad : actividades) {
            Label details = Ui.label(actividad.tipo() + " · " + (actividad.fecha() == null ? "Sin fecha" : actividad.fecha()), "caption");
            VBox text = new VBox(4, Ui.label(actividad.titulo(), "body"), details);
            if (actividad.descripcion() != null && !actividad.descripcion().isBlank()) text.getChildren().add(Ui.muted(actividad.descripcion()));
            Button remove = Ui.secondary("trash", "Eliminar");
            remove.setOnAction(e -> {
                if (confirmar("¿Eliminar “" + actividad.titulo() + "” de la planeación?")) {
                    clasesDao.eliminarActividad(actividad.id()); clasesTab.setContent(panelClases());
                }
            });
            // Genera el evento de clase en la agenda a partir de la actividad planificada.
            Button crearEnCalendario = null;
            if (actividad.fecha() != null) {
                crearEnCalendario = Ui.secondary("calendar", "Crear en calendario");
                crearEnCalendario.setOnAction(e -> crearEventoDesdeActividad(clase, actividad));
            }
            HBox row = new HBox(12, com.agendapersonal.ui.IconFactory.icon("calendar", 18), text, crearEnCalendario, remove);
            row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12));
            HBox.setHgrow(text, Priority.ALWAYS);
            box.getChildren().add(row);
        }
        return box;
    }

    /** Abre el formulario de evento con la fecha y el título de la actividad ya puestos (profesor confirma horas). */
    private void crearEventoDesdeActividad(ClaseDocente clase, ActividadClase actividad) {
        if (actividad.fecha() == null) { mostrarInfo("La actividad no tiene fecha: asigna una en la planeación antes de crear su evento."); return; }
        EventViews.formEnFechaHora(stage, null, actividad.fecha(), null, "Trabajo", controller,
            () -> clasesTab.setContent(panelClases()));
    }

    private VBox estudiantesCurso(ClaseDocente clase) {
        VBox box = section("Estudiantes y calificaciones", "Importa una lista .xlsx o administra los registros desde este curso. Reimportar actualiza por nombre para evitar duplicados.");
        TableView<EstudianteClase> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(clasesDao.listarEstudiantes(clase.id())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Ui.muted("Aún no hay estudiantes. Añádelos o importa un libro de Excel."));
        table.getColumns().add(columna("Estudiante", EstudianteClase::nombre));
        table.getColumns().add(columna("Correo", s -> vacio(s.correo())));
        table.getColumns().add(columna("Calificación", s -> s.calificacion() == null ? "—" : Double.toString(s.calificacion())));
        table.getColumns().add(columna("Observaciones", s -> vacio(s.observaciones())));
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox actions = new HBox(8);
        Button add = Ui.primary("plus", "Añadir estudiante"); add.setOnAction(e -> editarEstudiante(clase, null));
        Button edit = Ui.secondary("notes", "Editar seleccionado"); edit.setOnAction(e -> {
            EstudianteClase selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) mostrarInfo("Selecciona un estudiante primero."); else editarEstudiante(clase, selected);
        });
        Button delete = Ui.secondary("trash", "Eliminar"); delete.setOnAction(e -> {
            EstudianteClase selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) mostrarInfo("Selecciona un estudiante primero.");
            else if (confirmar("¿Eliminar a " + selected.nombre() + " de esta clase?")) { clasesDao.eliminarEstudiante(selected.id()); clasesTab.setContent(panelClases()); }
        });
        Button importar = Ui.secondary("download", "Importar Excel"); importar.setOnAction(e -> importarExcel(clase));
        Button exportar = Ui.secondary("book", "Exportar Excel"); exportar.setOnAction(e -> exportarExcel(clase));
        actions.getChildren().addAll(add, edit, delete, importar, exportar);
        box.getChildren().addAll(actions, table);
        return box;
    }

    private <T> TableColumn<EstudianteClase, String> columna(String title, java.util.function.Function<EstudianteClase, String> valor) {
        TableColumn<EstudianteClase, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(valor.apply(data.getValue())));
        return c;
    }

    private void crearClase() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Crear clase"); dialog.setHeaderText("Añade un curso al Espacio Docente");
        ButtonType save = new ButtonType("Crear clase", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField nombre = campo("Nombre del curso"); TextField grupo = campo("Grupo (opcional)"); TextField periodo = campo("Periodo (opcional)");
        VBox form = new VBox(10, Ui.label("Nombre", "body"), nombre, Ui.label("Grupo", "body"), grupo, Ui.label("Periodo", "body"), periodo);
        form.setPadding(new Insets(12)); dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == save ? save : null);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (nombre.getText().isBlank()) { mostrarInfo("Escribe el nombre del curso."); return; }
            if (clasesDao.crearClase(nombre.getText(), grupo.getText(), periodo.getText()) <= 0) mostrarError("No se pudo crear la clase.");
            else { clasesTab.setContent(panelClases()); tabs.getSelectionModel().select(clasesTab); }
        }
    }

    private void crearActividad(ClaseDocente clase) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Planeación de clase"); dialog.setHeaderText("Añade un elemento para " + clase.nombre());
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField titulo = campo("Título"); ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList("Clase", "Material", "Actividad", "Evaluación")); tipo.setValue("Clase"); tipo.setMaxWidth(Double.MAX_VALUE);
        DatePicker fecha = new DatePicker(); fecha.setMaxWidth(Double.MAX_VALUE); TextArea desc = new TextArea(); desc.setPromptText("Descripción o recordatorio (opcional)"); desc.setPrefRowCount(3);
        VBox form = new VBox(8, Ui.label("Título", "body"), titulo, Ui.label("Tipo", "body"), tipo, Ui.label("Fecha", "body"), fecha, desc); form.setPadding(new Insets(12)); dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(b -> b == save ? save : null);
        if (dialog.showAndWait().isPresent()) {
            if (titulo.getText().isBlank()) { mostrarInfo("Escribe el título."); return; }
            if (!clasesDao.crearActividad(clase.id(), titulo.getText(), tipo.getValue(), fecha.getValue(), desc.getText())) mostrarError("No se pudo guardar la actividad.");
        }
    }

    private void editarEstudiante(ClaseDocente clase, EstudianteClase existente) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(existente == null ? "Añadir estudiante" : "Editar estudiante");
        dialog.setHeaderText("Lista de " + clase.nombre()); ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField nombre = campo("Nombre completo"), correo = campo("Correo"); TextField nota = campo("Calificación (opcional)"); TextArea obs = new TextArea(); obs.setPromptText("Observaciones"); obs.setPrefRowCount(3);
        if (existente != null) { nombre.setText(existente.nombre()); correo.setText(vacio(existente.correo())); nota.setText(existente.calificacion() == null ? "" : Double.toString(existente.calificacion())); obs.setText(vacio(existente.observaciones())); }
        VBox form = new VBox(8, Ui.label("Nombre", "body"), nombre, Ui.label("Correo", "body"), correo, Ui.label("Calificación", "body"), nota, obs); form.setPadding(new Insets(12)); dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(b -> b == save ? save : null);
        if (dialog.showAndWait().isPresent()) {
            if (nombre.getText().isBlank()) { mostrarInfo("El nombre es obligatorio."); return; }
            Double grade = null;
            if (!nota.getText().isBlank()) try { grade = Double.valueOf(nota.getText().trim().replace(',', '.')); }
            catch (NumberFormatException ex) { mostrarInfo("La calificación debe ser un número."); return; }
            if (grade != null && !Double.isFinite(grade)) { mostrarInfo("La calificación debe ser un número válido."); return; }
            if (!clasesDao.guardarEstudiante(clase.id(), nombre.getText(), correo.getText(), grade, obs.getText())) mostrarError("No se pudo guardar el estudiante.");
            else clasesTab.setContent(panelClases());
        }
    }

    private void importarExcel(ClaseDocente clase) {
        FileChooser chooser = excelChooser("Importar lista de estudiantes"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File file = chooser.showOpenDialog(ventana()); if (file == null) return;
        try {
            ExcelClaseService.ResultadoImportacion resultado = ExcelClaseService.importar(file.toPath(), clase.id());
            int guardados = 0;
            for (EstudianteClase e : resultado.estudiantes()) if (clasesDao.guardarEstudiante(clase.id(), e.nombre(), e.correo(), e.calificacion(), e.observaciones())) guardados++;
            clasesTab.setContent(panelClases());
            mostrarInfo("Importación completada: " + guardados + " registros guardados." + (resultado.filasOmitidas() == 0 ? "" : " " + resultado.filasOmitidas() + " filas con calificación inválida se omitieron."));
        } catch (Exception ex) { mostrarError("No se pudo importar el libro: " + ex.getMessage()); }
    }

    private void exportarExcel(ClaseDocente clase) {
        FileChooser chooser = excelChooser("Exportar estudiantes y notas"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx")); chooser.setInitialFileName(nombreArchivo(clase.nombre()) + ".xlsx");
        File file = chooser.showSaveDialog(ventana()); if (file == null) return;
        File destino = file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx") ? file : new File(file.getParentFile(), file.getName() + ".xlsx");
        try { ExcelClaseService.exportar(destino.toPath(), clase, clasesDao.listarEstudiantes(clase.id())); mostrarInfo("Libro exportado a:\n" + destino.getAbsolutePath()); }
        catch (Exception ex) { mostrarError("No se pudo exportar el libro: " + ex.getMessage()); }
    }

    private static String nombreArchivo(String nombre) { return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim(); }
    private FileChooser excelChooser(String title) { FileChooser c = new FileChooser(); c.setTitle(title); c.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel .xlsx", "*.xlsx")); return c; }
    private Window ventana() { return root.getScene() == null ? null : root.getScene().getWindow(); }

    private VBox panelEvaluacion() {
        VBox panel = section("Evaluación y seguimiento", "Resumen de listas y calificaciones por curso, junto a tus tareas de revisión.");
        List<ClaseDocente> clases = clasesDao.listarClases();
        if (clases.isEmpty()) panel.getChildren().add(Ui.muted("Crea un curso desde la pestaña Clases para empezar a registrar estudiantes y notas."));
        for (ClaseDocente clase : clases) {
            List<EstudianteClase> estudiantes = clasesDao.listarEstudiantes(clase.id());
            long graded = estudiantes.stream().filter(s -> s.calificacion() != null).count();
            double avg = estudiantes.stream().filter(s -> s.calificacion() != null).mapToDouble(EstudianteClase::calificacion).average().orElse(Double.NaN);
            HBox row = new HBox(12, Ui.h3(clase.toString()), Ui.muted(estudiantes.size() + " estudiantes · " + graded + " calificados · promedio " + (Double.isNaN(avg) ? "—" : String.format(java.util.Locale.ROOT, "%.2f", avg))));
            row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12)); panel.getChildren().add(row);
        }
        Button add = Ui.primary("plus", "Añadir tarea de evaluación"); add.setOnAction(e -> TaskViews.form(null, controller, this::actualizar)); panel.getChildren().add(add);
        List<Tarea> tareas = controller.getTareas().stream().filter(t -> !t.estaCompletada()).toList();
        panel.getChildren().add(listadoTareas(tareas, "No hay tareas pendientes. Añade una fecha de entrega o una sesión de revisión."));
        return panel;
    }

    private VBox panelTutorias() {
        VBox panel = section("Tutorías y atención", "Reserva los espacios de atención como eventos y conserva acciones de seguimiento como tareas.");
        // Hoy y los próximos días: las tutorías programadas se ven en el calendario y aquí.
        panel.getChildren().add(listadoEventosRango(LocalDate.now(), 7,
            "No hay tutorías en los próximos días. Puedes programarlas desde Calendario."));
        Button add = Ui.secondary("calendar", "Ver calendario y crear tutoría"); add.setOnAction(e -> abrirCalendario.run()); panel.getChildren().add(add);
        return panel;
    }

    private VBox panelEnfoque() {
        VBox panel = section("Preparación concentrada", "Elige una técnica para planificar clases, revisar trabajos o preparar materiales sin interrupciones.");
        ComboBox<MetodoEnfoque> metodo = new ComboBox<>();
        metodo.getItems().addAll(MetodoEnfoque.PREDEFINIDOS);
        if (com.agendapersonal.util.Perfil.esDocente()) metodo.getItems().addAll(MetodoEnfoque.DOCENTES);
        metodo.setValue(metodo.getItems().get(0));
        metodo.setCellFactory(lv -> new ListCell<>() { @Override protected void updateItem(MetodoEnfoque item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.nombre()); }});
        metodo.setButtonCell(metodo.getCellFactory().call(null)); metodo.setMaxWidth(Double.MAX_VALUE);
        Label resumen = Ui.muted(metodo.getValue().resumen());
        metodo.setOnAction(e -> resumen.setText(metodo.getValue().resumen()));

        // En qué te vas a enfocar: según la técnica elegida se proponen opciones concretas (docente).
        ComboBox<String> foco = new ComboBox<>();
        List<String> focos = new ArrayList<>();
        Runnable actualizarFocos = () -> {
            List<String> n = focosDe(metodo.getValue());
            focos.clear(); focos.addAll(n);
            foco.getItems().clear();
            for (String f : n) foco.getItems().add(f);
            foco.setValue(n.isEmpty() ? null : n.get(0));
        };
        actualizarFocos.run();

        Button empezar = Ui.primary("play", "Iniciar bloque de enfoque"); empezar.setOnAction(e -> PantallaEnfoque.abrir(metodo.getValue(), foco.getValue(), PantallaEnfoque.Importancia.desde(ThemeService.get().settings().str("timeBlocking.importance")), List.of(), (item, hecho) -> {}));
        panel.getChildren().addAll(metodo, resumen, Ui.field("En qué te vas a enfocar", foco), new HBox(10, empezar)); return panel;
    }

    /** Opciones de enfoque para la técnica elegida: preparar una clase, planificar o corregir una tarea. */
    private List<String> focosDe(MetodoEnfoque m) {
        if (m == null) return List.of();
        if (m.propio()) return List.of("Trabajar en «" + m.nombre() + "»");
        List<String> r = new ArrayList<>();
        if ("docente-preparacion".equals(m.id()) || "docente-planeacion".equals(m.id())) {
            for (ClaseDocente cl : clasesDao.listarClases()) {
                r.add(("docente-preparacion".equals(m.id()) ? "Preparar «" : "Planificar «") + cl.nombre() + "»");
            }
        }
        if ("docente-evaluacion".equals(m.id())) {
            for (Tarea t : controller.getTareas()) {
                if (t.estaCompletada()) continue;
                r.add("Corregir: " + t.getTitulo());
            }
        }
        return r;
    }

    private VBox section(String title, String description) {
        VBox body = new VBox(14); body.setPadding(new Insets(20, 4, 8, 4)); Label heading = Ui.h2(title); Label info = Ui.muted(description); info.setWrapText(true); body.getChildren().addAll(heading, info); return body;
    }

    private VBox listadoEventos(LocalDate date, String emptyText) {
        return listadoEventosRango(date, 0, emptyText);
    }

    /** Eventos del rango [desde, desde + dias) ordenados por hora; usado por tutorías y agenda del docente. */
    private VBox listadoEventosRango(LocalDate desde, int dias, String emptyText) {
        VBox list = new VBox(8);
        LocalDate fin = desde.plusDays(dias);
        List<Evento> eventos = controller.getEventos().stream()
            .filter(e -> e.getFecha() != null && !e.getFecha().isBefore(desde) && e.getFecha().isBefore(fin))
            .sorted(java.util.Comparator.comparing(Evento::getHoraInicio, java.util.Comparator.nullsLast(String::compareTo))).toList();
        if (eventos.isEmpty()) list.getChildren().add(Ui.muted(emptyText));
        for (Evento evento : eventos) { Label when = Ui.label(evento.getHoraInicio() + "–" + evento.getHoraFin(), "caption"); VBox text = new VBox(3, Ui.label(evento.getTitulo(), "body"), when); if (evento.getDescripcion() != null && !evento.getDescripcion().isBlank()) text.getChildren().add(Ui.muted(evento.getDescripcion())); HBox row = new HBox(12, com.agendapersonal.ui.IconFactory.icon("calendar", 18), text); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12)); list.getChildren().add(row); }
        return list;
    }

    private VBox listadoTareas(List<Tarea> tareas, String emptyText) {
        VBox list = new VBox(8); if (tareas.isEmpty()) list.getChildren().add(Ui.muted(emptyText));
        for (Tarea tarea : tareas) { Label due = Ui.label(tarea.getFecha() == null ? "Sin fecha" : "Entrega: " + tarea.getFecha(), "caption"); VBox text = new VBox(3, Ui.label(tarea.getTitulo(), "body"), due); if (tarea.getDescripcion() != null && !tarea.getDescripcion().isBlank()) text.getChildren().add(Ui.muted(tarea.getDescripcion())); HBox row = new HBox(12, com.agendapersonal.ui.IconFactory.icon("tasks", 18), text); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12)); list.getChildren().add(row); }
        return list;
    }

    private boolean confirmar(String texto) { Alert a = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.CANCEL, ButtonType.OK); a.setHeaderText(null); return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent(); }
    private void mostrarInfo(String texto) { Alert a = new Alert(Alert.AlertType.INFORMATION, texto, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void mostrarError(String texto) { Alert a = new Alert(Alert.AlertType.ERROR, texto, ButtonType.OK); a.setHeaderText("No se completó la operación"); a.showAndWait(); }
    private static String vacio(String s) { return s == null ? "" : s; }
    private static TextField campo(String prompt) { TextField field = new TextField(); field.setPromptText(prompt); return field; }

    private void actualizar() { controller.cargarTodo(); clasesTab.setContent(panelClases()); tabs.getTabs().get(1).setContent(panelEvaluacion()); }
}
