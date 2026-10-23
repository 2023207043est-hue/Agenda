package com.agendapersonal;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.database.DatabaseConnection;
import com.agendapersonal.service.DataRefreshService;
import com.agendapersonal.service.TipoEntrada;
import com.agendapersonal.ui.FontPreferences;
import com.agendapersonal.ui.Logo;
import com.agendapersonal.ui.ThemeService;
import com.agendapersonal.ui.notify.BandejaSistema;
import com.agendapersonal.ui.notify.CentroAvisos;
import com.agendapersonal.ui.notify.NotificacionEscritorio;
import com.agendapersonal.ui.shell.AppShell;
import javafx.application.Application;
import com.agendapersonal.ui.Splash;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry-point Compás: abre la ventana principal y deja el programa avisando (eventos que empiezan,
 * cumpleaños del día) desde la bandeja del sistema aunque se cierre la ventana.
 */
public class Main extends Application {
    private MainController controller;
    private DataRefreshService live;
    private final BandejaSistema bandeja = new BandejaSistema();
    private CentroAvisos avisos;
    private AppShell shell;
    private boolean avisoBandejaMostrado;
    private Stage ventana;
    private Splash splash;

    @Override public void start(Stage stage) {
        // Pantalla de carga: se dibuja primero y cada paso pinta su mensaje antes de empezar.
        splash = Splash.carga();
        splash.mostrar();
        long inicio = System.currentTimeMillis();
        Runnable[] pasos = {
            () -> { splash.avance("Preparando tus datos…", 0.15);
                    DatabaseConnection.initializeDatabase(); },
            () -> { splash.avance("Cargando fuentes y tema…", 0.45);
                    FontPreferences.cargarFuentesIncluidas();
                    ThemeService.get().install(); },
            () -> { splash.avance("Preparando tu agenda…", 0.7);
                    controller = new MainController();
                    live = new DataRefreshService(controller); },
            () -> { splash.avance("Abriendo Compás…", 0.95);
                    // La pantalla de carga se queda al menos un instante para que no parpadee.
                    long resto = Math.max(0, 900 - (System.currentTimeMillis() - inicio));
                    PauseTransition p = new PauseTransition(Duration.millis(resto));
                    p.setOnFinished(e -> { splash.avance("Listo", 1); splash.cerrar(() -> {
                        Platform.runLater(() -> {
                            com.agendapersonal.ui.BienvenidaView.mostrarSiHaceFalta();
                            abrirVentana(stage);
                            Platform.runLater(() -> {
                                com.agendapersonal.ui.BienvenidaInicioView.mostrar(stage, controller,
                                    () -> shell.mostrar("Agenda Unificada"), () -> shell.mostrar("Notas y PKM"),
                                    () -> shell.mostrar("Espacio Docente"));
                                Platform.runLater(avisos::iniciar);
                            });
                        });
                    }); });
                    p.play(); }
        };
        ejecutar(pasos, 0);
    }

    /** Ejecuta un paso, deja respirar a la interfaz 70 ms para que pinte y sigue con el siguiente. */
    private void ejecutar(Runnable[] pasos, int i) {
        if (i >= pasos.length) return;
        PauseTransition p = new PauseTransition(Duration.millis(70));
        p.setOnFinished(e -> {
            try { pasos[i].run(); } catch (RuntimeException ex) { fallo(ex); return; }
            ejecutar(pasos, i + 1);
        });
        p.play();
    }

    private void fallo(RuntimeException e) {
        splash.cerrar(null);
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
            e.getMessage() + "\n\nRevisa el archivo compas.log en:\n" + DatabaseConnection.dataDir()).showAndWait();
        Platform.exit();
    }

    private void abrirVentana(Stage stage) {
        ventana = stage;
        Platform.setImplicitExit(false);          // cerrar la ventana NO termina el programa (sigue en la bandeja)
        stage.setTitle("Compás");
        Logo.aplicarIconos(stage);
        shell = new AppShell(controller, stage);
        Scene scene = new Scene(shell.getRoot(), 1320, 840);
        ThemeService.get().applyTo(scene);
        shell.instalarAtajos(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
        live.start();

        Runnable abrirAgenda = () -> {
            BandejaSistema.mostrar(stage);
            shell.mostrar("Agenda Unificada");
        };
        avisos = new CentroAvisos(controller, stage, abrirAgenda);
        NotificacionEscritorio.setAlAbrir(abrirAgenda);

        boolean enBandeja = bandeja.instalar(stage, avisos::mostrarResumenAhora, this::salir);
        shell.setAlSalir(this::salir);
        stage.setOnCloseRequest(e -> {
            if (!enBandeja) { e.consume(); salir(); return; }
            e.consume();
            stage.hide();
            if (!avisoBandejaMostrado) {
                avisoBandejaMostrado = true;
                NotificacionEscritorio.mostrar(TipoEntrada.EVENTO, "Compás sigue activo",
                    "Te avisará cuando empiece un evento. Ábrelo desde el ícono junto al reloj.", 7);
            }
        });
    }

    private boolean saliendo;

    /** Salida real del programa: la ventana se desvanece, aparece "Hasta pronto" y luego se cierra todo. */
    private void salir() {
        if (saliendo) return;
        saliendo = true;
        Runnable cerrarTodo = () -> {
            if (avisos != null) avisos.detener();
            if (live != null) live.stop();
            bandeja.quitar();
            NotificacionEscritorio.cerrarTodas();
            Platform.exit();
            System.exit(0);
        };
        Platform.runLater(() -> {
            NotificacionEscritorio.cerrarTodas();
            Stage v = ventana;
            if (v == null || !v.isShowing()) { Splash.despedida(com.agendapersonal.util.Perfil.nombre(), cerrarTodo); return; }
            FadeTransition f = new FadeTransition(Duration.millis(260), v.getScene().getRoot());
            f.setToValue(0);
            f.setOnFinished(e -> { v.hide(); Splash.despedida(com.agendapersonal.util.Perfil.nombre(), cerrarTodo); });
            f.play();
        });
    }

    public static void main(String[] args) { launch(args); }
}
