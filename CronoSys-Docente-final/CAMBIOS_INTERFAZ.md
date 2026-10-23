# Compás 1.1 — Interfaz renovada

Capturas de todas las pantallas (tema claro y oscuro) en [`docs/capturas`](docs/capturas).

## Diseño
- Nuevo estilo **claro y minimalista** basado en las referencias: fondo cálido, tarjetas blancas,
  menos bloques, encabezados limpios (ícono del módulo + título + frase + acción principal).
- Tema **oscuro** equivalente, pensado para leerse bien (contraste revisado).
- Cada módulo tiene su color: Tareas azul, Notas ámbar, Contactos violeta, Hábitos verde,
  Finanzas turquesa, Salud rosa, Time blocking naranja.
- Pantallas rediseñadas: Tareas y proyectos (agrupadas por Vencidas / Hoy / Mañana / Esta semana…),
  Notas (captura rápida + editor), Contactos (avatares, «Escríbele», cuenta regresiva de cumpleaños),
  Hábitos (semana en puntitos, racha, resumen del día), Finanzas (balance, gastos por categoría,
  selector de mes), Salud (tarjetas del día, registro rápido, historial), Time blocking y Configuración.
- Inicio, Calendario, Agenda unificada, diálogos, alertas, resumen del día y avisos de escritorio
  ahora usan los colores del tema (antes varios tenían colores oscuros fijos).

## Logo e íconos
- Logo nuevo: una «C» que también es la esfera de un reloj, con un punto que marca «ahora».
  Archivos en `src/main/resources/brand/` (PNG de 16 a 512 px, `compas.ico` para Windows y `compas-logo.svg`).
  Se usa en la ventana, la barra de tareas, la bandeja del sistema y el menú.
- 60+ íconos vectoriales nuevos (`ui/IconFactory.java`): se dibujan con CSS, así que cambian de color
  solos con el tema y el acento, y se ven nítidos a cualquier tamaño. Los PNG antiguos se retiraron.

## Menú lateral contraíble
- Botón «Contraer menú» (o **Ctrl+B**): el menú se anima hasta quedar solo con íconos; al pasar el
  mouse aparece el nombre de cada sección. La preferencia se guarda.

## Notificaciones
- **Avisos de cambios** dentro de la app (esquina superior derecha): al crear, guardar, completar,
  eliminar, cambiar el tema, etc. Entran deslizándose, se pausan con el mouse y se guardan en el
  historial de la **campana** (con contador de no leídos).
- **Avisos de escritorio solo en el momento**: cuando un evento empieza (ya no 15 minutos antes) y los
  cumpleaños del día. El resumen al abrir viene apagado (se activa en Configuración o desde la bandeja).
- Si otra persona o ventana cambia los datos, aparece «Datos actualizados».

## Animaciones
- Botones, filas y opciones con transición de color y efecto de pulsación.
- Las páginas entran con un fundido suave; las listas aparecen escalonadas.
- Marcar una tarea o un hábito hace un «pop»; los campos vacíos se sacuden al intentar guardar.
- El cambio de tema gira el ícono del sol/luna; el interruptor de Configuración se desliza.

## Fuentes y texto
- Fuentes incluidas (se ven igual en cualquier PC): **Plus Jakarta Sans** (por defecto), Inter,
  IBM Plex Sans, Source Sans 3, Roboto y JetBrains Mono, además de las del sistema. Licencias en
  `src/main/resources/fonts/LICENCIAS.md`.
- Nuevo ajuste **Tamaño del texto**: Pequeño, Normal, Grande y Muy grande.

## Errores corregidos
- **Cambio de apariencia**: el tema ahora se instala en todas las ventanas (principal, diálogos,
  alertas, resumen y avisos) y se reemplaza al instante; antes solo cambiaba la ventana principal y
  algunas piezas quedaban con los colores anteriores. Los títulos con fuente fija también se actualizan.
- La recarga automática cada 15 s ya no reinicia las listas si nada cambió (antes se perdía la selección).
- La base de datos se inicializa antes de cargar los datos (en una instalación nueva fallaba la primera carga).
- Contactos: el texto «Último contacto» ya no se pega (y se duplica) dentro de las notas al guardar.
- Hábitos: la racha ya no aparece en 0 durante el día si todavía no marcaste el hábito de hoy.
- Proyectos: al eliminar uno, sus tareas quedan «sin proyecto» en lugar de apuntar a un proyecto inexistente.
- Finanzas y Salud aceptan decimales con coma (12,50) y validan montos y rangos.
- Time blocking vuelve a estar en el menú y valida las horas (HH:mm, fin después del inicio).
- Fechas en formato 16/10/2026 en todos los formularios, sin depender del idioma del sistema.
- Los textos de botones ya no se cortan («Regis…») en ventanas estrechas.

## Atajos
- **Ctrl+B**: contraer/expandir el menú · **Ctrl+Shift+L**: claro/oscuro · **Ctrl+S**: guardar nota.
