package com.agendapersonal.ui;

import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Atajo histórico para mostrar un aviso breve. Ahora todo pasa por {@link Notifier}, que dibuja la
 * tarjeta con los colores del tema activo en la ventana principal (antes usaba colores fijos y
 * podía quedar mal ubicada si la ventana aún no tenía tamaño).
 */
public final class Toast {
    private Toast() {}

    public static void mostrar(Node origen, String mensaje) { Notifier.success(mensaje, null); }

    public static void mostrarGlobal(String mensaje) { Notifier.success(mensaje, null); }

    public static void mostrar(Window ventana, String mensaje) { Notifier.success(mensaje, null); }
}
