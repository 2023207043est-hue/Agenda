# Compás

![Compás](docs/capturas/logo-nombre-claro.png)

App de escritorio para organización personal integral: calendario, tareas y proyectos,
notas, contactos, hábitos, finanzas y salud. Interfaz clara y minimalista, con tema claro y
oscuro, seis colores de acento, seis fuentes incluidas y cuatro tamaños de texto.

> Novedades de la versión 1.1 (interfaz renovada, íconos, logo, menú contraíble, avisos y
> animaciones): ver [CAMBIOS_INTERFAZ.md](CAMBIOS_INTERFAZ.md).
>
> Novedades de la versión 1.6 (time blocking con métodos y rangos, pantalla de bloqueo
> personalizable, espacio de estudio local, logo nuevo y arreglo de las esquinas redondeadas):
> ver [CAMBIOS_TIMEBLOCKING.md](CAMBIOS_TIMEBLOCKING.md).

## Requisitos

- **JDK 25** (Eclipse Adoptium Temurin 25 recomendado)
- **Maven 3.9+**
- Windows con JavaFX 25 (las dependencias se descargan solas con Maven)

## Ejecutar

```powershell
mvn package -DskipTests
# Opción A (Maven):
mvn javafx:run
# Opción B (directa, sin plugin):
.\abrir-compas.bat
```

> `abrir-compas.bat` usa `JAVA_HOME`, o el JDK 25 portable en
> `%LOCALAPPDATA%\Programs\TemurinJDK25`. No requiere instalación ni admin.

## Innovaciones implementadas (lógica en `service/`, sin UI ni BD, con pruebas)

- **Detección de solapamientos** (`AgendaService`): calcula la intersección real de rangos y avisa
  cuántos minutos se cruzan; permite *ajustar* o *guardar de todos modos* y sugiere un hueco libre.
- **Duración automática** y validación estricta de horas (`HH:mm`, fin > inicio).
- **Progreso de proyectos** (`ProgresoService`): al completar una tarea se recalcula el % y se muestra
  un toast ("Progreso: 17% → 33%"). Cada proyecto muestra su % en la lista.
- **Dominio cerrado**: `Prioridad` y `EstadoTarea` son enums validados; fechas con `LocalDate`, horas con `LocalTime`.
- **CRM** (`ContactoService`): "Marcar contactado hoy", contactos que requieren seguimiento y cumpleaños próximos.
- **Time blocking** (`MetodoEnfoque` + `SesionEnfoque`): métodos con descripción y uso común
  (Pomodoro, 52/17, Deep work 60/10 y 90/20, Deep work 60–90 con rangos, ultradianos, Flowtime,
  Timeboxing) y métodos propios; los rangos se sortean al empezar la sesión.
- **Pantalla de bloqueo** (`PantallaEnfoque`): reloj, barra de progreso por fase, sesión y ciclo,
  pausa/reinicio/salto/detener, lista de pendientes que puede venir de las tareas o de las casillas
  de tus notas (y se guardan al marcarlas), fondo con imagen o video en bucle, audio local de la
  sesión y ajustes que se guardan en tu perfil.
- **Asistente local** (`AsistenteLocal`): reglas que leen tus tareas, notas, bloques y hábitos y te
  proponen por dónde empezar. Sin internet.
- **Horarios por perfil** (`SettingsStore`): varias franjas y días de la semana por perfil.

Pruebas: `mvn test` (JUnit 5, 81 pruebas).

## Estructura

- `src/main/java/com/agendapersonal/` — 86 fuentes
  - `domain/model` + `model` — entidades
  - `infrastructure/db` + `infrastructure/repo` + `dao` + `database` — SQLite + DAOs
  - `controller` + `application/service` + `service` — lógica y servicios
  - `ui` + `ui/shell` + `ui/modules` + `util` — vistas por módulo
- `src/main/resources/styles.css` — tema (variables que inyecta `ThemeService`)
- `Arquitectura_AgendaPersonal.pdf` — documento de arquitectura

## Base de datos

`agenda_personal.db` se crea sola al arrancar (`Database.migrate()`, 11 tablas).
No se versiona: cada desarrollador genera la suya localmente.

## Flujo colaborativo

```powershell
git pull            # traer cambios del compañero
mvn package -DskipTests
.\abrir-compas.bat
```

## Agenda Unificada y avisos (nuevo)

- **Agenda Unificada** (menú lateral): panel estilo Google Calendar con vistas Día / Semana / Mes / Agenda,
  mini-calendario, línea roja de "ahora" y botón *Crear*. Junta en un solo calendario cinco segmentos que se
  muestran/ocultan desde "Mis calendarios": **Eventos, Trabajos, Tareas, Hábitos, Cumpleaños**.
  - Trabajos = eventos de categoría *Trabajo* + tareas que pertenecen a un proyecto.
  - Cumpleaños salen de los contactos del CRM (se repiten cada año). Hábitos se marcan con un clic.
  - Lógica en `service/CalendarioUnificadoService` (con pruebas); UI en `ui/modules/AgendaUnificadaView`.
- **Resumen al iniciar**: ventana emergente con cumpleaños (hoy y próximos 7 días), eventos de hoy/mañana,
  tareas del día/vencidas y hábitos pendientes. Solo aparece si hay algo que recordar; se puede desactivar.
- **Notificaciones de escritorio**: tarjeta flotante en la esquina inferior derecha de la pantalla (siempre visible,
  aunque Compás esté minimizado) para eventos por empezar (por defecto 15 min antes) y cumpleaños.
- **Bandeja del sistema**: al cerrar la ventana el programa sigue activo junto al reloj. Menú: Abrir / Ver resumen / Salir.
  Código en `ui/notify/`.
# Perfil docente

Esta versión añade el perfil Docente y un Espacio Docente con pestañas para clases, evaluación,
tutorías y enfoque. Consulta [CAMBIOS_DOCENTES.md](CAMBIOS_DOCENTES.md) para el alcance y las
instrucciones de uso. El perfil se elige durante la bienvenida inicial y queda guardado; se cambia
desde Configuración. Al iniciar aparece un saludo con un recuerdo anterior, y Configuración permite
restablecer la aplicación tras confirmar el borrado de los datos.
