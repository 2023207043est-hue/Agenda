package com.agendapersonal.ui.notify;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Ícono de Compás en la bandeja del sistema (junto al reloj de Windows).
 * Permite cerrar la ventana y que el programa siga vivo avisando eventos y cumpleaños.
 */
public final class BandejaSistema {
    private TrayIcon icono;

    public boolean activa() { return icono != null; }

    /**
     * @return true si se instaló; false si el sistema no soporta bandeja (entonces cerrar la ventana cierra el programa).
     */
    public boolean instalar(Stage ventana, Runnable verResumen, Runnable salir) {
        try {
            if (!SystemTray.isSupported()) return false;
            PopupMenu menu = new PopupMenu();
            java.awt.MenuItem abrir = new java.awt.MenuItem("Abrir Compás");
            java.awt.MenuItem resumen = new java.awt.MenuItem("Ver resumen de hoy");
            java.awt.MenuItem cerrar = new java.awt.MenuItem("Salir");
            abrir.addActionListener(e -> Platform.runLater(() -> mostrar(ventana)));
            resumen.addActionListener(e -> Platform.runLater(verResumen));
            cerrar.addActionListener(e -> Platform.runLater(salir));
            menu.add(abrir);
            menu.add(resumen);
            menu.addSeparator();
            menu.add(cerrar);

            icono = new TrayIcon(crearImagen(), "Compás", menu);
            icono.setImageAutoSize(true);
            icono.addActionListener(e -> Platform.runLater(() -> mostrar(ventana)));   // doble clic
            SystemTray.getSystemTray().add(icono);
            return true;
        } catch (Exception | Error ex) {       // sin entorno gráfico AWT, etc.
            System.err.println("Bandeja no disponible: " + ex.getMessage());
            icono = null;
            return false;
        }
    }

    public static void mostrar(Stage ventana) {
        if (!ventana.isShowing()) ventana.show();
        ventana.setIconified(false);
        ventana.toFront();
        ventana.requestFocus();
    }

    /** Aviso nativo del sistema (globo de Windows); respaldo si la ventana flotante fallara. */
    public void globo(String titulo, String mensaje) {
        if (icono != null) icono.displayMessage(titulo, mensaje, TrayIcon.MessageType.INFO);
    }

    public void quitar() {
        try {
            if (icono != null) SystemTray.getSystemTray().remove(icono);
        } catch (Exception ignored) {}
        icono = null;
    }

    /** Logo de Compás (desde /brand); si no se encuentra, se dibuja uno simple por código. */
    private static java.awt.Image crearImagen() {
        try (var in = BandejaSistema.class.getResourceAsStream("/brand/compas-64.png")) {
            if (in != null) return javax.imageio.ImageIO.read(in);
        } catch (Exception ignored) { /* se usa el dibujo de respaldo */ }
        return dibujarRespaldo();
    }

    private static BufferedImage dibujarRespaldo() {
        int n = 64;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xC6, 0x4B, 0x1C));
        g.fillRoundRect(2, 2, n - 4, n - 4, 34, 34);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        var fm = g.getFontMetrics();
        g.drawString("C", (n - fm.stringWidth("C")) / 2, (n - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return img;
    }
}
