# Documentación técnica de la práctica 3

La práctica 3 amplía ToDoList con la gestión de equipos mediante tres historias:
008, listado de equipos; 009, gestión de pertenencia; y 010, edición y eliminación.
El desarrollo mantiene la arquitectura de Spring Boot, con entidades JPA,
repositorios, servicios transaccionales, DTO y vistas Thymeleaf. Las operaciones
se integran en la navegación existente y reutilizan la sesión de usuario.

## 008 Listado de equipos — issue #15

La [issue #15](https://github.com/isfs2-ua/mads-todolist/issues/15) incorpora la
entidad `Equipo`, con identificador, nombre y una colección de usuarios. La
relación entre equipos y usuarios es bidireccional y muchos a muchos, almacenada
en la tabla intermedia `equipo_usuario`. Esto permite que un usuario participe
en varios equipos y que cada equipo tenga múltiples miembros.

`EquipoRepository` proporciona el acceso a persistencia y `EquipoData` traslada
los datos a la capa web. `EquipoService` ofrece consultas para recuperar equipos,
obtener sus miembros y consultar los equipos de un usuario. Las conversiones
a DTO se realizan dentro de las transacciones para acceder correctamente a
las relaciones de carga diferida.

El menú autenticado añade «Equipos», que conduce a GET `/equipos`. El listado
muestra identificador y nombre, ordenado alfabéticamente. Pulsar un nombre abre
GET `/equipos/{id}/miembros`, donde aparecen identificador, nombre y correo de
los participantes, sin mostrar contraseñas. Ambas vistas incluyen mensajes
informativos cuando no hay equipos o participantes.

Estas consultas requieren una sesión de usuario existente, tanto normal como
administrador. Una sesión ausente o inválida produce HTTP 401; consultar los
miembros de un equipo inexistente devuelve HTTP 404.

## 009 Gestión de pertenencia — issue #16

La [issue #16](https://github.com/isfs2-ua/mads-todolist/issues/16) añade la
creación de equipos y las acciones para incorporarse o abandonarlos. Desde el
listado se accede al formulario GET `/equipos/nuevo`; el POST correspondiente
guarda el equipo mediante `EquipoService.crearEquipo()`. El nombre es obligatorio:
si es nulo, vacío o contiene únicamente espacios, se muestra el error conservando
el valor introducido. Tras crear el equipo se redirige al listado con un mensaje
de confirmación.

Cada fila ofrece «Unirme» cuando el usuario no pertenece al equipo y «Salir»
cuando ya es miembro. Estas operaciones usan POST `/equipos/{id}/unirse` y
POST `/equipos/{id}/salir`. El controller obtiene siempre el usuario de la sesión,
evitando que un parámetro permita modificar la pertenencia de otra persona.

El servicio rechaza incorporaciones duplicadas y salidas de usuarios que no
pertenecen al equipo. Abandonar un equipo conserva los demás miembros y las
otras pertenencias del usuario. Después de cada operación, el listado refleja
el cambio de botón y la consulta de participantes muestra los datos actualizados.

## 010 Gestión de equipos — issue #17

La [issue #17](https://github.com/isfs2-ua/mads-todolist/issues/17) permite al
administrador editar y eliminar equipos. Las opciones aparecen exclusivamente
para este rol en el listado. Además, los endpoints comprueban el permiso antes
de consultar el equipo, por lo que una petición directa sin autorización también
recibe HTTP 401.

La edición utiliza GET y POST `/equipos/{id}/editar`. El formulario muestra el
nombre actual; el servicio rechaza nombres vacíos o coincidentes con otro equipo,
pero admite guardar el mismo nombre. El cambio conserva los participantes y se
refleja en el listado y en la página de miembros. El identificador de la URL
determina el equipo modificado.

GET `/equipos/{id}/eliminar` muestra una página de confirmación con el nombre
del equipo. Cancelar vuelve al listado sin cambios. La eliminación solo se
ejecuta mediante POST con `confirmar=true`: desaparecen el equipo y sus relaciones
de pertenencia, conservando las cuentas y los demás equipos.

## Pruebas y alcance

El desarrollo de servicios se realizó incrementalmente con TDD. `EquipoTest`
comprueba persistencia y relaciones; `EquipoServiceTest`, reglas de negocio;
`EquipoWebTest` y `GestionEquiposWebTest`, formularios, permisos, mensajes y
conservación de datos. La validación final registrada en `CODEX.md` recoge
178 pruebas superadas, sin fallos ni errores. La suite se ejecuta con `./mvnw test`.

Quedan dos diferencias respecto a los requisitos: la creación todavía no valida
la unicidad del nombre, aunque la edición sí lo hace; y las opciones administrativas
están en el listado, en lugar de la página individual del equipo.
