# Personalización – fases 1 y 2 (v1.3)

## Qué quedó hecho
- `settings/` (Java puro, con 11 pruebas en `SettingsStoreTest`): JSON versionado con perfiles, migración desde
  `ui.theme / ui.accent / ui.font / ui.fontSize / ui.sidebar.collapsed / reloj.* / usuario.nombre`, valores
  inválidos → por defecto, exportar/importar con validación, horario de perfiles, cálculo de contraste y paleta derivada.
- Tokens: colores derivados de fondo/tarjeta/texto/acento, `-rN` (radios, escalados por "radio de esquinas"),
  `-dv-btn/-h-btn/-dv-menu/-h-menu` (densidad). `styles.css` usa esos tokens (con los valores por defecto se ve igual).
- 27 temas (los 2 originales conservan su paleta exacta), Crear tema, acento hex/paleta, escala 85–125 %,
  radio 0–28, densidad, luz cálida, reducir animaciones, tema por hora, perfiles, exportar/importar/restablecer.

## Pendiente (fases 3–7)
Panel Personalizar por ventana, modo edición del Inicio, menú lateral editable, resto de opciones por ventana.

# v1.4 – segunda tanda
- **Calendario + Agenda unificada**: una sola ventana «Calendario» (usa la interfaz de la agenda unificada; incluye el calendario de Cumpleaños de tus contactos).
- **Notificaciones**: se eliminan avisos duplicados (mismo aviso en <2 s), horas de silencio (Configuración → Horas de silencio); las confirmaciones de guardado siguen apagadas por defecto.
- **Tareas**: «Tareas y proyectos» → «Tareas»; «Proyectos» → «Listas». Hora opcional, destacar con estrella, viñeta de importancia (rojo/naranja/verde) y vínculo con un evento y con un bloque de time blocking. Migración de BD v4.
- **Contactos**: apodo, categorías propias (escribe una nueva en el desplegable) y fechas importantes extra. Migración de BD v4.
- **Time blocking**: métodos Pomodoro, 52/17, Deep work 60/10 y 90/20, ultradianos, Flowtime, Timeboxing y métodos propios; pantalla de enfoque con reloj, barra de progreso, sesión/ciclo, pausar/reiniciar/detener, tareas de hoy, fondo (degradado, color, imagen o video en bucle) e importancia (Normal / Alta: pausa si sales / Estricta: pantalla completa). Nueva dependencia `javafx-media` en el pom.

## Pendiente
Importar calendarios (.ics / Google), calendarios editables con notificaciones y repeticiones, reloj del Inicio con más opciones, importar Excel en Finanzas, espacio de estudio con IA, Spotify.

# v1.5
- **Pantalla de carga** (`ui/Splash`): logo, mensaje por paso y barra de progreso; se desvanece al abrir.
- **Salida elegante**: botón «Salir» en la barra superior y Ctrl+Q (con confirmación). La ventana se desvanece y aparece «Hasta pronto, <nombre>» antes de cerrar. La X sigue dejando Compás en la bandeja.
- **Avisos**: más compactos, con franja de color por tipo (verde, azul, ámbar, rojo) y sombra más marcada.
