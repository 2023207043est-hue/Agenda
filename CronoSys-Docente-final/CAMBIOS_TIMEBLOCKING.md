# Time blocking y Espacio de estudio (v1.6)

Todo lo de esta tanda funciona **sin internet**: nada sale de tu equipo.

## Métodos de bloqueo

Cada método tiene nombre, descripción corta y **uso común**. Todos vienen de serie:

| Método | Trabajo | Descanso | Ciclo | Uso típico |
|---|---|---|---|---|
| Pomodoro | 25 min | 5 min | 4 sesiones → descanso largo de **15 a 30 min** | estudiar, tareas administrativas, arrancar cuando cuesta |
| 52/17 | 52 min | 17 min | — | oficina sostenida: redactar, programar, analizar |
| Deep work 60/10 | 60 min | 10 min | — | concentración profunda |
| Deep work 90/20 | 90 min | 20 min | — | proyectos grandes (tesis, código) |
| **Deep work 60–90** | **60–90 min** | **10–20 min** | — | bloque largo con duración variable |
| Ciclos ultradianos | 90 min | 20 min | 4 ciclos → 30 min | jornadas largas cuidando la energía |
| Flowtime | sin límite | 1/5 de lo trabajado (mín. 5) | — | creatividad, ritmo ininterrumpido |
| Timeboxing | 45 min | 5 min | — | cortar el perfeccionismo |

- **Rangos**: los métodos con rango (60–90 / 10–20 / 15–30) sortean un valor concreto **al empezar la
  sesión** y la pantalla dice cuál ha tocado. Se puede fijar a mano un valor exacto.
- **Métodos propios**: se crean con nombre, descripción, uso común, trabajo (fijo o rango), descanso
  (fijo o rango), sesiones por ciclo y descanso largo (fijo o rango). Se pueden duplicar y eliminar.
  Los que tú creas se guardan en tu perfil.

## Pantalla de bloqueo

Reloj grande, barra de progreso (cambia de color según la fase: trabajo / descanso / descanso largo),
número de sesión y de ciclo, y botones de **iniciar-pausar**, **reiniciar el tiempo**, **saltar fase**,
**terminar sesión** (Flowtime) y **detener**.

- **Pendientes de la sesión**: elige de dónde salen (tareas y notas / solo tareas / solo notas / ninguno).
  De las notas se sacan las casillas: `- [ ] algo`, `[ ] algo`, `☐ algo` (y `[x]` / `☑` como hechas).
  **Marcar una casilla en la pantalla la guarda en tu nota**; marcar una tarea la completa.
- **Botón «Agregar fondo» dentro de la propia pantalla**: imagen (png, jpg, gif, bmp, webp) o video
  (mp4, m4v, webm, ogv), con repetición en bucle, color o degradado del tema.
- **Botón «Ajustes» dentro de la propia pantalla**: tamaño del reloj, mostrar/ocultar la barra, el
  detalle y la lista de pendientes, velo sobre el fondo, sonido al cambiar de fase, repetición del
  video, **audio local de la sesión** (un mp3/wav/ogg de tu equipo: se repite mientras trabajas y
  calla en los descansos) e importancia. Todo se guarda en tu perfil.
- **Importancia**:
  - *Normal*: puedes salir y volver cuando quieras.
  - *Alta*: si minimizas o cambias de ventana, el contador se pausa.
  - *Estricta*: pantalla completa, ventana siempre al frente, contador en pausa al salir y atajos de
    teclado bloqueados dentro de la app (Escape abre el aviso para detener).

> **Límite honesto**: una app de escritorio en Java no puede tapar ni cerrar otros programas de
> Windows (haría falta una app del sistema con permisos de administrador). Lo que sí hace el modo
> Estricta es lo de arriba.

## Espacio de estudio

Un panel nuevo en el menú lateral, hecho de piezas que tú eliges (**Personalizar el espacio**):

- **Enfoque rápido**: método + texto y a empezar la sesión sin salir de ahí.
- **Sugerencias**: un asistente **local** que lee tus tareas, notas, bloques y hábitos y te dice por
  dónde empezar (qué venció, cuántas casillas te quedan, qué método encaja con lo que tienes entre
  manos, si estás dentro de un bloque de enfoque…). Sin internet y sin mandar nada.
- **Pendientes**: las tareas de hoy y las casillas de tus notas, marcables ahí mismo.
- **Bloques de hoy**, **Hábitos de hoy**, **Notas rápidas** y **Tu día en números**.

Los paneles se pueden quitar, poner y ordenar; se guarda en tu perfil.

## Horarios más personalizables

Los perfiles ya no tienen una sola franja: pueden tener **varias franjas** (por ejemplo 08:00–12:00 y
16:00–18:00) y **días de la semana** concretos (lunes a viernes, o los que elijas). Si no marcas
ningún día, el perfil se activa esos días todos. Configuración → Perfiles.

## Archivos

- `service/MetodoEnfoque.java` — métodos con rangos, descripción y uso común.
- `service/SesionEnfoque.java` — máquina de estados con la duración sorteada.
- `service/ListaPendientes.java` — casillas de las notas y tareas (lógica pura, con pruebas).
- `service/AsistenteLocal.java` — el asistente local (lógica pura, con pruebas).
- `ui/modules/PantallaEnfoque.java` — la pantalla de bloqueo.
- `ui/modules/TimeBlockingView.java` — configuración de sesiones, métodos, fondo y audio.
- `ui/modules/EspacioEstudioView.java` — el espacio de estudio.
- `settings/SettingsStore.java` — horarios con varias franjas y días.

## Pruebas

`mvn test` → 81 pruebas (las nuevas cubren rangos de métodos, casillas de notas y horarios múltiples).

# v1.6 (segunda parte) – logo nuevo y arreglo de los avisos del log

## La marca: Compás

- **Nuevo nombre**: la app se llama **Compás** (brújula y, a la vez, compás musical: el ritmo del día).
  Se han actualizado todos los textos de la interfaz, las ventanas, la bandeja del sistema y la
  documentación. El nombre interno del paquete (`com.agendapersonal`), la base de datos
  (`%APPDATA%\CronoSys\agenda_personal.db`) y el formato de los archivos exportados **no se tocan**:
  tus datos siguen donde estaban y los respaldos antiguos se siguen importando.
- **Nuevo logo**: una brújula con la aguja saliendo por la abertura del anillo, sobre el azulejo
  naranja de siempre. Mismo diseño en todos los sitios: vectorial en la app (`ui/Logo.java`),
  SVG (`brand/compas-logo.svg`), PNG de 16 a 512 px y `brand/compas.ico` para Windows.
- La cabecera del README (`docs/capturas/logo-nombre-*.png`) se ha regenerado con el logo nuevo.
- `abrir-cronosys.bat` → `abrir-compas.bat`.

## Arreglo del CSS (las esquinas redondeadas y los avisos del log)

Los avisos que aparecían al abrir la app (`ClassCastException: Double cannot be cast to Size`) y las
esquinas que **no** se redondeaban tenían la misma causa: en JavaFX una variable propia de CSS
(`-r12`, `-dv-btn`…) solo se convierte a **colores**; usada como tamaño (`-fx-border-radius: -r12`)
no se aplica y salía el aviso de conversión.

La solución, en `ui/ThemeService.java`:

1. `styles.css` y la hoja del tema se **fusionan en una sola** (se sustituye el bloque `.root` de
   styles.css por el del tema activo), porque con dos hojas JavaFX pierde el valor de las variables.
2. Al construir la hoja, los tokens de forma (`-rN`, `-dv-…`, `-h-…`) se **sustituyen por números
   literales**, ya escalados con el ajuste «radio de esquinas» y «densidad».

Resultado: las esquinas se redondean (tarjetas, botones, campos, píldoras) y el log de arranque
queda limpio. Además, los ajustes «radio de esquinas» (0–28 px) y «densidad» **ahora sí se notan**,
porque antes los radios se ignoraban.

## Otros arreglos

- El diálogo de bienvenida ya no revienta al arrancar: `showAndWait` no puede llamarse desde el
  manejador de una animación, así que se pide en el siguiente pulso.
- El desplegable de método del «Enfoque rápido» del Espacio de estudio mostraba el registro entero
  (`MetodoEnfoque[id=pomodoro, …]`); ahora muestra solo el nombre.
