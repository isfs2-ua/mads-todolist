# Documentación técnica de la práctica 3

## 008 Listado de equipos

La [issue #15](https://github.com/isfs2-ua/mads-todolist/issues/15) solicita una
opción «Equipos» en el menú, un listado alfabético con enlaces a cada equipo
y una página con sus participantes. Un usuario puede pertenecer a varios equipos.
Las páginas deben informar cuando no hay equipos o participantes.

Se trabaja en la rama `listado-equipos`, mediante incrementos pequeños. Cada
commit incorpora su documentación y se espera la indicación del usuario antes
de preparar el siguiente. La integración en `main` se realizará mediante PR.

### Primer incremento: modelo y persistencia

Se crea la entidad `Equipo`, con identificador generado y nombre obligatorio
no vacío. Se guarda en la tabla `equipos`. La relación bidireccional
`@ManyToMany` con `Usuario` utiliza la tabla intermedia `equipos_usuarios`,
con las columnas `equipo_id` y `usuario_id`. `Equipo` es el propietario de la
relación; `Usuario.equipos` utiliza `mappedBy = "usuarios"`.

Los métodos `Equipo.addUsuario()` y `Usuario.addEquipo()` mantienen ambos lados
en memoria. Las colecciones son conjuntos para evitar pertenencias repetidas.
No se configura persistencia ni borrado en cascada: los usuarios se guardan
independientemente y se persisten las asociaciones desde el equipo. Las
colecciones se cargan de forma diferida, por lo que su consulta debe realizarse
dentro de una transacción.

`EquipoRepository` hereda las operaciones de `CrudRepository` e incorpora
`findAllByOrderByNombreAsc()`. Spring Data genera la consulta ordenada por
nombre ascendente; el criterio de comparación depende de la base de datos.
Sin equipos devuelve una lista vacía. No se exige que los nombres sean únicos.

Ejemplo: Ana pertenece a Desarrollo y Soporte, mientras que Juan pertenece a
Desarrollo. Se guardan dos equipos, dos usuarios y tres asociaciones. Consultar
Desarrollo devuelve a Ana y Juan; consultar los equipos de Ana devuelve ambos.

Se adapta `clean-db.sql` para borrar las asociaciones y los equipos antes de
los usuarios, respetando las claves foráneas y el aislamiento de las pruebas.
`EquipoTest` cubre la persistencia de un equipo vacío, la relación de varios
usuarios con varios equipos sin duplicados, el orden alfabético y el listado
vacío. Las pruebas de persistencia vacían el contexto JPA antes de recuperar
los datos, para comprobar lo almacenado en la base de datos.

Validación con H2:

```bash
./mvnw -Dtest=EquipoTest,UsuarioTest,UsuarioServiceTest test
```

Resultado: **24 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`), incluidas las cuatro pruebas nuevas. La ejecución cubre
también el modelo y servicio de usuarios existentes. No se ha ejecutado este
incremento con el perfil PostgreSQL.

Quedan pendientes los DTO y servicios de consulta, las páginas del listado y
del equipo, los mensajes informativos y el enlace del menú.

### Segundo incremento: DTO y servicios de consulta

Se incorpora `EquipoData`, con identificador y nombre, y `ParticipanteData`,
con identificador, nombre y correo. Este último contiene únicamente los datos
necesarios para presentar a los participantes; no incluye la contraseña, los
roles ni las relaciones de la entidad `Usuario`.

`EquipoService` ofrece tres consultas con `@Transactional(readOnly = true)`:

- `findAll()` convierte el resultado ordenado del repositorio a una lista de
  `EquipoData`. Si no existen equipos, devuelve una lista vacía.
- `findById(equipoId)` devuelve los datos del equipo o `null` si no existe,
  siguiendo el criterio de las consultas por identificador de usuarios y tareas.
- `allUsuariosEquipo(equipoId)` devuelve los participantes ordenados por
  identificador. Un equipo sin participantes devuelve una lista vacía; si el
  equipo no existe, lanza `EquipoServiceException`. Así se distingue un equipo
  vacío de un identificador inexistente.

La conversión con ModelMapper se realiza dentro de la transacción, incluida
la lectura de la colección diferida de usuarios. Los futuros controladores
recibirán DTO independientes de la sesión de persistencia.

`EquipoServiceTest` incorpora siete pruebas de integración con H2, sin
`@Transactional` en los tests. Cubren el orden y los datos del listado, la
ausencia de equipos, la búsqueda existente e inexistente, los participantes
de un equipo vacío, el error al consultar participantes de un equipo inexistente
y la pertenencia múltiple. Este último caso recupera a Ana en Desarrollo y
Soporte, a Juan solamente en Desarrollo y a un tercer usuario solamente en
Soporte; comprueba también un nombre opcional ausente.

Validación con H2:

```bash
./mvnw -Dtest=EquipoServiceTest,EquipoTest,UsuarioServiceTest test
```

Resultado: **24 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`): siete del servicio de equipos, cuatro de su persistencia
y trece del servicio de usuarios. `git diff --check` pasa sin incidencias.
No se ha ejecutado este incremento con el perfil PostgreSQL.

Quedan pendientes los controladores, las plantillas del listado y del equipo,
los mensajes informativos en las páginas y el enlace «Equipos» del menú.
