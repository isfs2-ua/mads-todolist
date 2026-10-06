t# Contexto del proyecto ToDoList

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
| GET / POST | `/registro` | Muestra formulario / registra un usuario y redirige a login. |
| GET | `/logout` | Elimina el identificador de usuario de la sesión y redirige a login. |
| GET | `/about` | Página pública «Acerca de». |
| GET | `/usuarios/{id}/tareas` | Lista tareas del usuario. |
| GET / POST | `/usuarios/{id}/tareas/nueva` | Formulario / creación de tarea. |
| GET / POST | `/tareas/{id}/editar` | Formulario / modificación del título. |
| DELETE | `/tareas/{id}` | Borra una tarea; responde con cuerpo vacío. |

`ManagerUserSession` almacena `idUsuarioLogeado` en `HttpSession`. El login correcto redirige actualmente a `/usuarios/{id}/tareas`; el registro no inicia sesión automáticamente. No se usa Spring Security.

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
