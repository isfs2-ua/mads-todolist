# Documentación técnica de la práctica 2

## 002 Barra de menú

### Primer incremento: navegación para visitantes en «Acerca de»

Se ha añadido el fragmento Thymeleaf `menuVisitante` a `fragments.html`. Utiliza
una Navbar de Bootstrap con el enlace «ToDoList» a `/about` y los enlaces «Login»
y «Registro» a sus respectivos formularios. Estos dos últimos se sitúan a la
derecha mediante la clase `ml-auto`.

La plantilla `about.html` incorpora la barra antes del contenido con:

```html
<nav th:replace="fragments :: menuVisitante"></nav>
```

`th:replace` sustituye este elemento por el fragmento común, lo que permite
reutilizar el menú sin duplicar su estructura. No ha sido necesario modificar
controllers, servicios ni el modelo de datos.

Se ha ampliado `AcercaDeWebTest` con una petición GET a `/about` sin sesión.
La prueba verifica una respuesta HTTP 200 y que los tres enlaces de navegación
tienen el destino y el texto esperados en el HTML renderizado.

Este primer incremento cubría la navegación para visitantes.

### Segundo incremento: menú autenticado en «Acerca de»

`HomeController` consulta el identificador de sesión mediante `ManagerUserSession`
y recupera el DTO del usuario con `UsuarioService.findById`. Lo incorpora al
modelo como `usuario`; si no hay sesión o el usuario ya no existe, el valor es
nulo y se mantiene el menú para visitantes.

Se ha creado el fragmento `menuUsuario(usuario)` con enlaces a «Acerca de» y a
las tareas del usuario. A la derecha aparece un desplegable con su nombre,
«Cuenta» deshabilitado como opción futura y «Cerrar sesión», que enlaza a
`/logout`. El desplegable utiliza el JavaScript de Bootstrap ya incluido.

La plantilla selecciona el fragmento según el usuario disponible:

```html
<nav th:replace="${usuario != null} ? ~{fragments :: menuUsuario(${usuario})} : ~{fragments :: menuVisitante}"></nav>
```

La nueva prueba de `AcercaDeWebTest` registra un usuario y realiza la petición
con una sesión HTTP que contiene su identificador. Comprueba el nombre y los
enlaces del menú autenticado, y la ausencia de enlaces de login y registro.
La prueba sin sesión también comprueba que no aparece el enlace de logout.
Los datos de las pruebas se limpian mediante `clean-db.sql`.

En este segundo incremento quedaba pendiente incorporar el menú a las páginas
de tareas.

### Tercer incremento: menú común en las páginas de tareas

Las plantillas `listaTareas.html`, `formNuevaTarea.html` y `formEditarTarea.html`
incorporan al principio del cuerpo el mismo fragmento autenticado:

```html
<nav th:replace="fragments :: menuUsuario(${usuario})"></nav>
```

El listado y el formulario de creación ya recibían el DTO `usuario`.
`TareaController.formEditaTarea` ahora también recupera al propietario y lo añade
al modelo, después de comprobar que coincide con el usuario autenticado.

La prueba `paginasDeTareasMuestranMenuDelUsuario` de `TareaWebTest` recorre las
tres rutas y verifica el nombre y los enlaces del menú. Utiliza un usuario
persistido y simula su autenticación mediante `ManagerUserSession`, siguiendo
el enfoque de las pruebas existentes.

Con este incremento, el menú está integrado en las páginas actuales de tareas
y en «Acerca de». Login y registro siguen sin barra. Se completa el alcance
funcional de la issue #3; «Cuenta» permanece como opción futura.

## 003 Listado de usuarios

La [issue #4](https://github.com/isfs2-ua/mads-todolist/issues/4) solicita que
la URL `/registrados` muestre el identificador y el correo electrónico de los
usuarios registrados.

### Primer incremento: servicio de consulta del listado

Se ha añadido `UsuarioService.findAll()`, que devuelve una
`List<UsuarioData>` con todos los usuarios registrados. Reutiliza el método
`findAll()` heredado de `CrudRepository` en `UsuarioRepository` y convierte
cada entidad `Usuario` al DTO existente mediante `ModelMapper`, siguiendo el
patrón de las demás consultas del servicio.

La consulta utiliza `@Transactional(readOnly = true)`. Cuando no hay usuarios,
devuelve una lista vacía. No se establece un orden de presentación en este
incremento ni se modifica el repositorio o el modelo de datos.

Se han añadido dos pruebas de integración a `UsuarioServiceTest`:

- `servicioListadoUsuariosDevuelveIdentificadoresYCorreos` registra dos usuarios
  y comprueba que el listado contiene exactamente sus identificadores y correos,
  sin depender del orden de los resultados.
- `servicioListadoUsuariosSinUsuariosDevuelveListaVacia` comprueba la consulta
  sobre una base de datos sin usuarios.

Las pruebas utilizan la base de datos H2 de pruebas y `clean-db.sql`, que limpia
las tareas y los usuarios antes de cada prueba. El comando de validación es:

```bash
./mvnw -Dtest=UsuarioServiceTest test
```

Resultado de la validación: **8 pruebas ejecutadas, 0 fallos, 0 errores y
0 omitidas** (`BUILD SUCCESS`), incluidas las dos pruebas nuevas.

La ruta `/registrados`, su controlador y su plantilla quedan pendientes del
segundo incremento. La integración del menú corresponde al tercero.
