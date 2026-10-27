# Contexto del proyecto ToDoList

## Historia 010: condiciones de satisfacción y eliminación web (26/10/2026)

Esta actualización prevalece sobre los permisos de los incrementos iniciales:
solo los administradores pueden editar o eliminar equipos.

- El listado muestra «Editar equipo» y «Eliminar equipo» únicamente al administrador.
  Los GET y POST de edición y eliminación comprueban ese permiso antes de consultar
  el equipo. Usuarios normales reciben HTTP 401 con el motivo de administrador;
  los accesos sin sesión válida también reciben HTTP 401. No basta con ocultar botones.
- GET `/equipos/{id}/eliminar` muestra `confirmarEliminarEquipo.html` con el nombre
  del equipo, la pregunta de confirmación y «Cancelar», que vuelve al listado.
  Consultar o cancelar esta página conserva el equipo y sus participantes.
- POST `/equipos/{id}/eliminar` requiere `confirmar=true`. Entonces llama al servicio
  y redirige al listado con «Equipo eliminado correctamente». Sin confirmación no
  elimina nada. Los equipos inexistentes devuelven HTTP 404 al confirmar o consultar.
- `GestionEquiposWebTest` comprueba visibilidad, peticiones directas sin permisos,
  confirmación, cancelación, eliminación de pertenencias, conservación de cuentas y
  de otros equipos, y la indisponibilidad del equipo borrado. `EquipoWebTest` comprueba
  la edición como administrador, validaciones, nombre actualizado y miembros conservados.
- Validación final: 99 pruebas de equipos y suite completa de 178 pruebas superadas,
  sin fallos ni errores; `git diff --check` sin errores.

## Historia 010: guardado de equipos (26/10/2026)

- POST `/equipos/{id}/editar` usa `EquipoController.guardarEquipo()` para modificar
  el nombre y redirigir al listado con «Equipo modificado correctamente».
- Comprueba la sesión antes de buscar el equipo (HTTP 401); un equipo inexistente
  devuelve HTTP 404. El identificador de la URL determina el equipo que se modifica,
  aunque la petición incluya otro identificador.
- Los errores de nombre vacío o duplicado se muestran en `formEditarEquipo.html`,
  conservando el nombre recibido y la acción del formulario con el equipo correcto.
- Las pruebas cubren usuario normal y administrador, persistencia del nombre,
  conservación de miembros, nombre sin cambios, errores, permisos y actualización
  del listado y de la vista de miembros. Pasan 90 pruebas de servicio y controller
  de equipos; `git diff --check` sin errores.
- La eliminación web queda para el siguiente incremento.

## Historia 010: primer incremento web (26/10/2026)

- La rama `gestion-equipo` ya dispone de `EquipoService.modificarEquipo()` y
  `eliminarEquipo()`, desarrollados con TDD.
- GET `/equipos/{id}/editar` muestra `formEditarEquipo.html` con el nombre actual,
  menú del usuario y enlace para cancelar. El listado incluye una columna «Gestión»
  con el enlace «Editar» para cada equipo.
- `EquipoController.formEditarEquipo()` requiere sesión válida antes de buscar el
  equipo. Admite usuarios normales y administradores; devuelve HTTP 401 sin usuario
  válido y HTTP 404 si el equipo no existe. `EquipoWebTest` cubre estos casos.
- El guardado del formulario y la eliminación web quedan para incrementos posteriores.

## Historia 009: creación de equipos en la web (25/10/2026)

- GET `/equipos/nuevo` muestra `formNuevoEquipo.html` con el campo de nombre,
  el menú del usuario y un enlace para cancelar. El listado enlaza al formulario.
- `EquipoController.formNuevoEquipo()` requiere sesión de un usuario existente;
  admite usuarios normales y administradores. Los accesos sin sesión o con un
  usuario inexistente devuelven HTTP 401. `EquipoWebTest` cubre estos casos.
- POST `/equipos/nuevo` usa `EquipoController.crearEquipo()` para guardar el nombre
  mediante el servicio y redirigir a `/equipos` con «Equipo creado correctamente».
  Si el nombre es nulo, vacío o solo contiene espacios, vuelve al formulario con
  el nombre recibido y el error del servicio; no se guarda ningún equipo.
- El POST también exige sesión válida. Las pruebas cubren creación por usuario normal
  y administrador, errores de nombre y accesos sin sesión o con usuario inexistente.
  Validación del incremento: 42 pruebas de servicio y controller de equipos superadas.
- La creación, incorporación y salida de equipos están implementadas en incrementos separados.

## Historia 009: incorporación a equipos (25/10/2026)

- El listado recibe `equiposUsuario` y ofrece un formulario «Unirme» solo para equipos
  a los que no pertenece el usuario. Los demás ofrecen «Salir».
- POST `/equipos/{id}/unirse` usa únicamente el identificador del usuario de la sesión;
  permite usuarios normales y administradores. Sin sesión válida devuelve HTTP 401,
  antes de buscar el equipo. Un equipo inexistente devuelve HTTP 404.
- Tras incorporarse redirige al listado con confirmación. Si ya pertenece al equipo,
  redirige con el error del servicio, sin modificar la pertenencia.
- `EquipoWebTest` cubre incorporación, cambio del botón, mensajes, usuario de sesión,
  petición duplicada, equipo inexistente, acceso no autorizado y rechazo de GET.
  Validación: 50 pruebas de servicio y controller de equipos superadas.

## Historia 009: salida de equipos (25/10/2026)

- POST `/equipos/{id}/salir` elimina la pertenencia del usuario de la sesión y
  redirige al listado con «Has salido del equipo correctamente». El botón «Salir»
  aparece solo si pertenece al equipo; tras salir vuelve a aparecer «Unirme».
- Requiere sesión válida (HTTP 401 antes de buscar el equipo). Un equipo inexistente
  devuelve HTTP 404; intentar salir sin pertenecer al equipo redirige con el error
  del servicio. GET no modifica pertenencias y devuelve HTTP 405.
- Las pruebas cubren usuario normal y administrador, cambio de botones, mensajes,
  errores y permisos. También comprueban que un parámetro `idUsuario` no permite
  expulsar a otro miembro y que se conservan los otros miembros y equipos.
- Validación: 58 pruebas de servicio y controller de equipos superadas;
  `git diff --check` sin errores.

## Actualización del 25/10/2026: miembros de equipos (segundo incremento)

- GET `/equipos/{id}/miembros` usa `EquipoController.miembrosEquipo()` y la vista
  `miembrosEquipo.html`. El nombre de cada equipo en `/equipos` enlaza a esta ruta.
- La vista muestra nombre del equipo e identificador, nombre y correo de sus miembros,
  sin contraseñas, con mensaje para equipos vacíos y enlace de vuelta al listado.
- Cualquier usuario con sesión válida puede consultar miembros. La autenticación se
  comprueba antes de buscar el equipo; un equipo inexistente devuelve HTTP 404
  mediante `EquipoNotFoundException`.
- `EquipoWebTest` incorpora pruebas de miembros, exclusión de usuarios de otros equipos,
  ausencia de contraseñas, equipo vacío, administrador, equipo inexistente y accesos
  sin sesión o con usuario inexistente; también verifica los enlaces desde el listado.
- Validación: las 11 pruebas de `EquipoWebTest` y la suite completa de 95 pruebas
  pasan sin fallos ni errores; `git diff --check` sin errores.

## Actualización del 25/10/2026: listado de equipos (primer incremento)

- La rama `listado-equipos` incorpora GET `/equipos` mediante `EquipoController.listadoEquipos()`
  y la vista `listaEquipos.html`, con identificador, nombre y mensaje de listado vacío.
- El listado reutiliza `EquipoService.findAllOrdenadoPorNombre()` y requiere una sesión
  de usuario existente, tanto normal como administrador; en otro caso devuelve HTTP 401.
- El menú común de usuario incluye un enlace a «Equipos».
- `EquipoWebTest` cubre listado ordenado, menú, listado vacío, administrador y accesos
  sin sesión o con usuario inexistente. La consulta de miembros queda para el siguiente incremento.
- Validación del incremento: 5 pruebas nuevas y suite completa de 89 pruebas superadas;
  `git diff --check` sin errores.

## Actualización del 12/10/2026

Esta actualización prevalece sobre las descripciones históricas del 06/10/2026
que aparecen más abajo:

- `main` incluye el menú común, el listado `/registrados` y la descripción
  `/registrados/{id}`, enlazada desde el listado y sin mostrar la contraseña.
- La versión Maven vigente es `1.1.0-SNAPSHOT`.
- La rama `usuario-administrador` implementa la issue #6 («005 Usuario
  administrador») en tres incrementos: modelo y servicio, formulario y login.
  `UsuarioData` y `RegistroData` incorporan `administrador`; `Usuario` expone
  `isAdministrador()` y lo persiste como `TRUE` o `NULL` en una columna única
  para impedir dos administradores. `UsuarioService.existeAdministrador()`
  consulta su existencia y `registrar()` rechaza un segundo administrador.
- `/registro` ofrece el checkbox sólo mientras no exista un administrador,
  también al volver a mostrar el formulario con errores. Un POST directo para
  crear otro administrador vuelve al formulario con un error de negocio.
- Tras un registro correcto se inicia sesión automáticamente con el DTO
  persistido. `LoginController.iniciarSesion()` se comparte con el login:
  administrador a `/registrados` y usuario normal a sus tareas. Los errores de
  registro no abren una sesión autenticada. Esta mejora pasa **17 pruebas de
  registro, login y bloqueo**.
- Tras el login, el administrador va a `/registrados` y el usuario normal a
  sus tareas. En los dos incrementos de #7, la rama `proteccion-usuarios`
  restringe `/registrados` y `/registrados/{id}` al administrador mediante
  `UsuarioController.comprobarAdministrador()`: sin sesión, con un usuario normal
  o con una sesión de usuario inexistente devuelve HTTP 401 y el motivo
  «No autorizado: se necesita permiso de administrador». En la ficha se comprueban
  los permisos antes de buscar el usuario solicitado; un administrador recibe
  HTTP 404 si la ficha no existe.
- El primer incremento de #8, en `bloqueo-usuarios`, añade `bloqueado` a `Usuario`
  y `UsuarioData`, inicialmente `false`, y el método transaccional
  `UsuarioService.cambiarBloqueo(usuarioId, bloqueado)`.
  Validación de este incremento: **19 pruebas de servicio y repositorio superadas**.
- El segundo incremento de #8 añade POST `/registrados/{id}/bloqueo`, con el
  parámetro booleano `bloqueado`, y botones «Bloquear»/«Habilitar» en el listado.
  El endpoint comprueba administrador y existencia antes de cambiar el estado,
  y redirige al listado. No se ofrece bloqueo para el administrador y un POST
  directo para bloquearlo devuelve HTTP 400.
  Validación: **20 pruebas de bloqueo web, listado y servicio superadas**.
- El tercer incremento de #8 añade `UsuarioService.LoginStatus.USER_BLOCKED`:
  tras verificar las credenciales, una cuenta bloqueada no inicia sesión.
  `LoginController` muestra «Tu cuenta está bloqueada. Contacta con el
  administrador.» en el formulario existente. Habilitar la cuenta permite
  acceder de nuevo. Las sesiones previamente abiertas no se invalidan.
- `PaginaErrorResolver` selecciona `error.html` para las respuestas HTML de
  error. La plantilla usa Bootstrap y muestra código HTTP, título en español,
  motivo y enlaces a login y about. Conserva los motivos de los errores 4xx;
  los fallos internos muestran un mensaje genérico. `PaginaErrorWebTest`
  comprueba las páginas HTTP 401 y 404 con un servidor en un puerto aleatorio
  y el mensaje genérico de HTTP 500 en el resolver.
- `doc/practica2.md` documenta los incrementos y sus pruebas. La última ejecución
  de la suite completa, anterior a la mejora de sesión automática tras el registro,
  pasó con **73 pruebas, sin fallos ni errores**; `git diff --check` también pasa.

## 1. Propósito y fuentes

Aplicación académica de gestión de tareas de la asignatura MADS de la Universidad de Alicante. Permite registrar usuarios, iniciar y cerrar sesión y gestionar las tareas de cada usuario. Parte de `ccanoe/mads-todolist-inicial` y evoluciona mediante las prácticas de la asignatura.

Este documento sirve como referencia para futuras peticiones. Describe el estado observado el **06/10/2026**; antes de implementar un issue, comprobar su contenido y estado actuales y contrastarlos con el código local.

Fuentes de referencia:

- [Enunciado de la práctica 2](https://ccanoe.github.io/apuntes-mads/practicas/parte2-todolist/).
- [Repositorio GitHub](https://github.com/isfs2-ua/mads-todolist).
- [Issues del repositorio](https://github.com/isfs2-ua/mads-todolist/issues?q=is%3Aissue).
- [Tablero Trello enlazado en el README](https://trello.com/b/xYyf7yRf/todolist-mads). Su contenido no se ha revisado para redactar este documento.
- Código local, `pom.xml`, configuración, plantillas y pruebas.

Los requisitos funcionales pendientes se detallan en los issues enlazados en la sección 6. Los nombres de autor y de proyecto de los ejemplos del enunciado deben adaptarse a este repositorio.

## 2. Estado actual

- Autor indicado en la aplicación: **Ismael Sánchez Faz**.
- Coordenadas Maven: `es.ua.mads:mads-todolist-isfs2:1.0.1`.
- Página `/about`: versión **1.0.1**, fecha de release **29/09/2026**.
- Rama principal: `main`.
- Registro, login, logout y operaciones de listado, creación, edición y borrado de tareas implementados.
- Página «Acerca de» implementada; issue #1 cerrado y PR #2 integrado según el historial local.
- Issues #3–#8 abiertos; todavía no hay menú común, páginas `/registrados`, administrador ni bloqueo de usuarios en el código revisado.
- La siguiente versión prevista por el enunciado es **1.1.0**. El `pom.xml` local todavía declara `1.0.1`.

La implementación actual guarda y compara contraseñas directamente, sin hashing. `UsuarioData` también contiene la contraseña: al desarrollar la descripción de usuarios, excluirla explícitamente de la vista y de cualquier respuesta pública.

## 3. Tecnologías y estructura

Proyecto Maven con Java 8 como referencia del README y del Dockerfile, Spring Boot **2.7.14**, Spring MVC, Thymeleaf, Spring Data JPA/Hibernate, Bean Validation, H2 y ModelMapper **3.0.0**. Usa imports `javax.*`. La interfaz utiliza recursos locales de Bootstrap **4.6.0**, jQuery y Popper; no hay un proyecto frontend separado ni `package.json`.

```text
pom.xml                                  Dependencias, versión y construcción
mvnw / mvnw.cmd / .mvn/wrapper/           Maven Wrapper (Maven 3.6.3)
Dockerfile                               Imagen para ejecutar el JAR
src/main/java/madstodolist/
  Application.java                       Arranque de Spring Boot
  authentication/ManagerUserSession.java Gestión de la sesión HTTP
  config/ModelMapperConfig.java           Bean de conversión entre entidades y DTO
  controller/                            Rutas, formularios y respuestas MVC
    exception/                           Excepciones con estado HTTP
  dto/                                   Datos de formularios y transferencia
  model/                                 Entidades Usuario y Tarea
  repository/                            Interfaces Spring Data
  service/                               Lógica de negocio y transacciones
src/main/resources/
  application.properties                 Configuración de desarrollo
  messages.properties                    Mensajes de validación
  templates/                             Vistas Thymeleaf y fragmentos comunes
  static/css/ y static/js/                Recursos de la interfaz
src/test/java/madstodolist/
  repository/                            Pruebas de entidades y persistencia
  service/                               Pruebas de negocio
  controller/                            Pruebas HTTP con MockMvc
src/test/resources/
  application.properties                 Configuración de pruebas
  clean-db.sql                           Limpieza de tareas y usuarios
```

### Responsabilidades de las capas

- **Controller:** recoge y valida entradas, comprueba acceso, llama a servicios y prepara el modelo o la redirección. `LoginController` gestiona autenticación y registro; `TareaController`, tareas; `HomeController`, `/about`.
- **Service:** concentra reglas de negocio y acceso transaccional a las entidades. `UsuarioService` registra, autentica y consulta usuarios; `TareaService` gestiona tareas y comprueba pertenencia; `InitDbService` carga ejemplos en el perfil `dev`.
- **Repository:** `UsuarioRepository` y `TareaRepository` extienden `CrudRepository`. El primero añade `findByEmail`, que devuelve un `Optional<Usuario>`.
- **DTO:** `UsuarioData` y `TareaData` trasladan datos a los controllers; `LoginData` y `RegistroData` representan formularios. ModelMapper convierte entre entidades y DTO.
- **Vista:** formularios y listados en Thymeleaf. `fragments.html` contiene los fragmentos `head(titulo)` y `javascript`; todavía no contiene una barra de navegación.

### Modelo y persistencia

`Usuario` se guarda en `usuarios`: `id`, `email`, `nombre`, `password` y `fechaNacimiento`. `Tarea` se guarda en `tareas`: `id`, `titulo` y referencia al usuario mediante `usuario_id`. Los identificadores son `Long`, generados con `IDENTITY`.

La relación es bidireccional: un usuario tiene un `Set<Tarea>` y una tarea pertenece a un usuario. `Usuario.addTarea` y `Tarea.setUsuario` mantienen ambos lados. La colección de tareas es lazy. Como `spring.jpa.open-in-view=false`, acceder a relaciones lazy y convertirlas a DTO dentro de los servicios transaccionales, antes de devolver datos al controller.

En desarrollo se usa `jdbc:h2:mem:dev`, esquema `update` y perfil `dev`. Los datos desaparecen al detener la aplicación. `InitDbService` crea el usuario de ejemplo `user@ua` con contraseña `123` y las tareas «Lavar coche» y «Renovar DNI». La consola H2 está en `/h2-console`.

Las pruebas usan `jdbc:h2:mem:test`, esquema `create` y no activan el perfil `dev`. Varias clases ejecutan `clean-db.sql` mediante `@Sql` para aislar sus datos.

## 4. Rutas y autenticación existentes

| Método | Ruta | Comportamiento |
| --- | --- | --- |
| GET | `/` | Redirige a `/login`. |
| GET / POST | `/login` | Muestra formulario / comprueba credenciales y abre sesión. |
| GET / POST | `/registro` | Muestra formulario / registra, inicia sesión y redirige según el rol. |
| GET | `/logout` | Elimina el identificador de usuario de la sesión y redirige a login. |
| GET | `/about` | Página pública «Acerca de». |
| GET | `/usuarios/{id}/tareas` | Lista tareas del usuario. |
| GET / POST | `/usuarios/{id}/tareas/nueva` | Formulario / creación de tarea. |
| GET / POST | `/tareas/{id}/editar` | Formulario / modificación del título. |
| DELETE | `/tareas/{id}` | Borra una tarea; responde con cuerpo vacío. |

`ManagerUserSession` almacena `idUsuarioLogeado` en `HttpSession`. El login y el registro correctos inician sesión y redirigen a `/usuarios/{id}/tareas` para usuarios normales o a `/registrados` para el administrador. No se usa Spring Security.

`TareaController` compara el usuario de la sesión con el propietario del recurso. Para edición y borrado, obtiene primero la tarea y su propietario. El acceso sin autorización lanza `UsuarioNoLogeadoException` (**HTTP 401**); una tarea inexistente lanza `TareaNotFoundException` (**HTTP 404**). Mantener estas comprobaciones al modificar los flujos de tareas.

## 5. Ejecución y comprobaciones

Desde la raíz del proyecto, con un JDK compatible instalado:

```bash
# Ejecutar la aplicación en localhost:8080
./mvnw spring-boot:run

# Ejecutar todas las pruebas
./mvnw test

# Ejecutar una clase de pruebas concreta
./mvnw -Dtest=UsuarioServiceTest test

# Generar el JAR, ejecutando las pruebas
./mvnw package

# Ejecutar el artefacto de la versión actual
java -jar target/mads-todolist-isfs2-1.0.1.jar
```

Abrir `http://localhost:8080/login`. El nombre de JAR mostrado en el README corresponde al proyecto inicial y está desactualizado: obtener el nombre vigente a partir de `artifactId` y `version` en `pom.xml`. En Windows se puede usar `mvnw.cmd`.

El Dockerfile copia `target/*.jar`, por lo que hay que empaquetar antes de construir la imagen. En la copia local revisada utiliza `eclipse-temurin:8-jdk-alpine` y configura `java.security.egd` para arrancar el JAR.

```bash
docker build -t mads-todolist:local .
docker run --rm -p 8080:8080 mads-todolist:local
```

Las pruebas existentes usan JUnit 5, AssertJ y las herramientas de Spring Boot. Los tests web emplean MockMvc; `UsuarioWebTest` simula `UsuarioService` con `@MockBean`, mientras que `TareaWebTest` utiliza servicios y datos de prueba reales. `AcercaDeWebTest` comprueba que `/about` contiene «ToDoList».

Elegir pruebas según el cambio y cubrir reglas de negocio, respuestas HTTP, redirecciones y contenido relevante. Para autorización, comprobar también peticiones directas sin sesión o con un usuario sin permisos. El método `asignarEtiquetaATarea` de `TareaServiceTest` sólo comprueba pertenencia de una tarea: su nombre no implica que exista una funcionalidad de etiquetas.

## 6. Issues y requisitos pendientes

Estado consultado el **06/10/2026**: siete issues en total, uno cerrado y seis abiertos. El número **#2 corresponde a un pull request**, no a un issue de funcionalidad.

| Issue | Estado | Alcance |
| --- | --- | --- |
| [#1 · Acerca de](https://github.com/isfs2-ua/mads-todolist/issues/1) | Cerrado | Autor, versión, fecha y personalización del nombre Maven; implementado. |
| [#3 · Barra de menú](https://github.com/isfs2-ua/mads-todolist/issues/3) | Abierto | Navegación común y variante pública en `/about`. |
| [#4 · Listado de usuarios](https://github.com/isfs2-ua/mads-todolist/issues/4) | Abierto | Página `/registrados` con identificador y correo. |
| [#5 · Descripción de usuario](https://github.com/isfs2-ua/mads-todolist/issues/5) | Abierto | Detalle en `/registrados/{id}`, sin contraseña. |
| [#6 · Usuario administrador](https://github.com/isfs2-ua/mads-todolist/issues/6) | Abierto | Registro de un único administrador y acceso al listado. |
| [#7 · Protección de usuarios](https://github.com/isfs2-ua/mads-todolist/issues/7) | Abierto | Listado y detalle accesibles sólo para el administrador. |
| [#8 · Bloqueo de usuarios](https://github.com/isfs2-ua/mads-todolist/issues/8) | Abierto | Bloquear y habilitar usuarios desde el listado; impedir login bloqueado. |

### Criterios funcionales de los issues

- **#3:** Navbar superior de Bootstrap en las páginas de la aplicación excepto login y registro. A la izquierda, «ToDoList» enlaza a `/about` y «Tareas» a las tareas del usuario. A la derecha, nombre del usuario y desplegable con «Cuenta» (funcionalidad futura) y «Cerrar sesión <nombre usuario>». En `/about`, mostrar este menú cuando haya sesión y enlaces a login y registro cuando no la haya. No desarrollar una gestión de cuenta completa sólo por este issue.
- **#4:** mostrar en `/registrados` los identificadores y correos de los usuarios registrados.
- **#5:** enlazar desde el listado al detalle de cada usuario. Mostrar sus datos salvo la contraseña. El enunciado del issue escribe `/registrados/:id`; en Spring MVC se expresa como `/registrados/{id}`.
- **#6:** permitir solicitar el alta como administrador mediante un checkbox de registro. Ocultarlo si ya existe administrador y garantizar también en el servicio que no se cree un segundo mediante una petición directa. Tras autenticarse, el administrador debe acceder al listado de usuarios.
- **#7:** proteger tanto listado como detalle en el servidor. Ante usuarios sin permiso, devolver «No autorizado» y un mensaje explicativo, siguiendo el patrón HTTP 401 existente. Ocultar enlaces por sí solo no cumple el requisito.
- **#8:** ofrecer al administrador botones para bloquear o habilitar cada usuario. El login de un usuario bloqueado debe fallar y explicar el motivo. El issue no concreta qué ocurre con sesiones ya abiertas ni con el bloqueo del propio administrador; resolver esos casos según el alcance de la petición futura.

Los issues #3–#5 corresponden a las funcionalidades obligatorias del enunciado y #6–#8 a sus ampliaciones opcionales. Todas tienen un issue creado, pero eso no supone autorización para implementarlas todas en cada petición. Como orden técnico sugerido: #4 antes de #5; #6 antes de #7; #8 se apoya en el listado y los permisos de administrador. #3 puede abordarse de forma independiente.

## 7. Metodología y entregables de la práctica

El enunciado propone una rama por issue, integración mediante PR hacia `main`, commits explicativos y relación entre issues, etiquetas de historia, milestones y PR. Utiliza Trello para historias de usuario y GitHub Projects para seguimiento técnico. No impone GitFlow en esta parte.

Para preparar la versión 1.1.0, mantener coherentes versión y fecha en `pom.xml` y `about.html`, generar la release y validar la imagen Docker según la petición de entrega. Los nombres de imagen y cuentas deben ser los del usuario.

La entrega también requiere **`doc/practica2.md`**, una documentación técnica en Markdown de **500–800 palabras**, sin contar código, con cambios, pruebas y un ejemplo explicado. Este archivo aún no existe; `CODEX.md` cumple una función distinta y no sustituye ese entregable. Incorporar tests de servicios y presentación para las nuevas funcionalidades.

## 8. Pautas para futuras peticiones

1. Leer este documento, el issue afectado y los archivos de las capas implicadas; verificar el estado de Git y preservar cambios locales ajenos a la tarea.
2. Mantener la arquitectura controller → service → repository, con DTO para los datos de presentación y transacciones en los servicios. Reutilizar fragmentos Thymeleaf para elementos comunes.
3. Mantener la compatibilidad con Java 8 y Spring Boot 2.7 salvo que se solicite una migración. Evitar introducir nuevas dependencias o un frontend separado sin necesidad.
4. Aplicar autorización en el servidor y comprobar reglas como administrador único y bloqueo en la capa de negocio correspondiente.
5. Escribir explicaciones, documentación y textos de interfaz en español, respetando los nombres existentes del código.
6. Ejecutar las comprobaciones pertinentes y comunicar qué se validó y qué quedó sin verificar. Actualizar esta referencia cuando cambien rutas, arquitectura o funcionalidades.
7. Realizar commits, publicaciones, modificaciones de issues o acciones sobre tableros cuando formen parte del alcance autorizado por el usuario.

Este documento se ha redactado mediante revisión del código, la práctica y los issues; no acredita una ejecución de la aplicación ni el resultado de la suite de pruebas.
