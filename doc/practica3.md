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
