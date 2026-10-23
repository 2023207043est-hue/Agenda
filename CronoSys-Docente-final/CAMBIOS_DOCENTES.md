# Perfil docente y Espacio Docente

La aplicación ofrece ahora dos espacios seleccionables: **Estudiante** y **Docente**.

- En una instalación nueva, la bienvenida permite escoger el perfil junto con el nombre.
- El perfil elegido queda guardado y Compás abre directamente ese espacio en los siguientes inicios.
- El nombre y el perfil se pueden editar en **Configuración → Tu perfil**.
- Al abrir la aplicación aparece un saludo personalizado y un recuerdo tomado de una nota anterior, una tarea completada o un evento pasado.
- El perfil Estudiante mantiene el Espacio de Estudio existente.
- El perfil Docente muestra el **Espacio Docente**, organizado en pestañas: Clases, Evaluación, Tutorías y Enfoque. En Clases se crean cursos propios, cada uno con paneles de Resumen, Planeación y Estudiantes y notas.
- Cada curso puede registrar estudiantes, correo, calificación y observaciones, además de clases, materiales, actividades y evaluaciones con fecha.
- **Importar Excel** y **Exportar Excel** intercambian la lista y las notas del curso como libros `.xlsx`. La importación reconoce encabezados como Estudiante/Nombre, Correo/Email, Calificación/Nota y Observaciones/Comentarios; al repetir una importación se actualiza por nombre en ese curso.
- Las pestañas utilizan la agenda y las tareas actuales. Las acciones de Time Blocking enlazan las técnicas generales con tres opciones docentes: Preparación de clase 50/10, Corrección 40/10 y Planeación profunda 90/20.
- **Configuración → Restablecer Compás** ofrece una acción protegida por confirmación para borrar registros, perfiles y preferencias, y volver a pedir nombre y perfil.

## Alcance del perfil

Compás es una aplicación local de un solo perfil, sin inicio de sesión con contraseña ni cuentas independientes. La selección inicial define el espacio de trabajo; ambos espacios usan la misma agenda y tareas, y el Espacio Docente añade cursos, listas y calificaciones locales. La integración con Excel lee y escribe archivos `.xlsx`; no controla una sesión abierta de Microsoft Excel ni sincroniza en vivo con OneDrive.

## Ejecutar

El proyecto conserva Maven y Java 25. Desde esta carpeta:

```powershell
mvn javafx:run
```

Los métodos docentes solo se incorporan a la lista cuando el perfil activo es Docente; los métodos estudiantiles y los métodos propios guardados continúan disponibles.
