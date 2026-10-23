package com.agendapersonal.ui.notify;

import com.agendapersonal.controller.MainController;
import com.agendapersonal.infrastructure.db.ConfigDAO;
import com.agendapersonal.service.AgendaDatos;
import com.agendapersonal.service.CalendarioUnificadoService;
import com.agendapersonal.service.RecordatorioService;
import com.agendapersonal.service.RecordatorioService.Aviso;
import com.agendapersonal.service.RecordatorioService.Resumen;
import com.agendapersonal.ui.Notifier;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordina los recordatorios de escritorio. Solo avisa de lo que pasa <b>en el momento</b>:
 * <ul>
 *   <li>un evento que está empezando (a su hora de inicio, no antes);</li>
 *   <li>los cumpleaños del día (una sola vez, al abrir o al cambiar de día).</li>
 * </ul>
 * El resumen del día ya no aparece solo al iniciar (se puede activar en Configuración o abrir desde
 * el ícono de la bandeja). Cada aviso queda también en el historial de la campana.
 */
public final class CentroAvisos {
    public static final String CFG_RESUMEN = "inicio.resumen";    // "1" muestra el resumen al abrir; por defecto "0"
    public static final String CFG_MINUTOS = "aviso.minutos";     // antelación en minutos; por defecto 0 = al empezar
    public static final String CFG_EVENTOS = "aviso.eventos";     // "1" (por defecto) o "0"
    public static final String CFG_CUMPLES = "aviso.cumples";     // "1" (por defecto) o "0"
    private static final int VENTANA_CUMPLES_DIAS = 7;

    private final MainController c;
    private final Stage ventana;
    private final Runnable irAgenda;
    private final ConfigDAO cfg = new ConfigDAO();
    private final Set<String> avisados = ConcurrentHashMap.newKeySet();
    private volatile boolean ocupado;
    private Timeline reloj;
    private LocalDate ultimoDia = LocalDate.now();

    public CentroAvisos(MainController c, Stage ventana, Runnable irAgenda) {
        this.c = c; this.ventana = ventana; this.irAgenda = irAgenda;
    }

    public void iniciar() {
        LocalDate hoy = LocalDate.now();
        ultimoDia = hoy;
        var datos = datos(hoy);
        if ("1".equals(cfg.get(CFG_RESUMEN, "0"))) {
            Resumen r = RecordatorioService.resumenInicio(hoy, datos, VENTANA_CUMPLES_DIAS);
            if (r.hayNoticias()) mostrarResumen(r);
        }
        verificar(true);
        // Se revisa cada 20 s para que el aviso salga lo más cerca posible de la hora exacta.
        reloj = new Timeline(new KeyFrame(Duration.seconds(20), e -> tick()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
    }

    public void detener() { if (reloj != null) reloj.stop(); }

    /** Muestra la ventana de resumen a pedido (menú de la bandeja). */
    public void mostrarResumenAhora() {
        mostrarResumen(RecordatorioService.resumenInicio(LocalDate.now(), datos(LocalDate.now()), VENTANA_CUMPLES_DIAS));
    }

    private void mostrarResumen(Resumen r) {
        ResumenInicioView.mostrar(ventana.isShowing() ? ventana : null, r, irAgenda,
            noMostrar -> cfg.set(CFG_RESUMEN, noMostrar ? "0" : "1"), "1".equals(cfg.get(CFG_RESUMEN, "0")));
    }

    private void tick() {
        LocalDate hoy = LocalDate.now();
        boolean nuevoDia = !hoy.equals(ultimoDia);
        if (nuevoDia) { ultimoDia = hoy; avisados.clear(); }   // pasó la medianoche con el programa abierto
        verificar(nuevoDia);
    }

    /**
     * Revisa qué hay que avisar ahora. Las consultas a la base de datos (hábitos, contactos, configuración)
     * se hacen en un hilo de fondo para no congelar la ventana; solo mostrar el aviso ocurre en el hilo de JavaFX.
     */
    private void verificar(boolean incluirCumples) {
        if (ocupado) return;
        List<com.agendapersonal.model.Evento> eventos = List.copyOf(c.getEventos());   // copia hecha en el hilo de JavaFX
        List<com.agendapersonal.model.Tarea> tareas = List.copyOf(c.getTareas());
        ocupado = true;
        Thread t = new Thread(() -> {
            try {
                LocalDateTime ahora = LocalDateTime.now();
                boolean eventosOn = !"0".equals(cfg.get(CFG_EVENTOS, "1"));
                boolean cumplesOn = incluirCumples && !"0".equals(cfg.get(CFG_CUMPLES, "1"));
                List<Aviso> pendientes = new ArrayList<>();
                if (eventosOn || cumplesOn) {
                    var datos = AgendaDatos.cargar(eventos, tareas, ahora.toLocalDate(), ahora.toLocalDate());
                    if (cumplesOn) pendientes.addAll(RecordatorioService.avisosCumpleHoy(ahora.toLocalDate(), datos, avisados));
                    if (eventosOn) pendientes.addAll(RecordatorioService.avisosEventos(ahora, datos, minutos(), avisados));
                }
                if (!pendientes.isEmpty()) Platform.runLater(() -> mostrarAvisos(pendientes));
            } catch (RuntimeException e) {
                com.agendapersonal.util.Log.error("CentroAvisos", e);
            } finally { ocupado = false; }
        }, "avisos");
        t.setDaemon(true);
        t.start();
    }

    private void mostrarAvisos(List<Aviso> lista) {
        for (Aviso a : lista) {
            if (!avisados.add(a.clave())) continue;      // ya se avisó (dos revisiones seguidas)
            NotificacionEscritorio.mostrar(a);
            Notifier.history().addFirst(new Notifier.Entry(Notifier.Tone.INFO, a.titulo(), a.mensaje(), LocalDateTime.now()));
            Notifier.unreadProperty().set(Notifier.unreadProperty().get() + 1);
        }
    }

    private int minutos() {
        try { return Math.max(0, Math.min(240, Integer.parseInt(cfg.get(CFG_MINUTOS, "0").trim()))); }
        catch (NumberFormatException e) { return 0; }
    }

    private CalendarioUnificadoService.Datos datos(LocalDate hoy) {
        return AgendaDatos.cargar(c, hoy, hoy);
    }
}
