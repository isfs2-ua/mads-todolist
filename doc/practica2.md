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

### Segundo incremento: página de usuarios registrados

Se ha creado `UsuarioController` con el endpoint GET `/registrados`. El método
`listadoUsuarios` consulta `UsuarioService.findAll()`, añade la colección al
modelo con el atributo `usuarios` y devuelve la vista `listaUsuarios`.
La ruta es accesible sin sesión, tal como establece el alcance acordado para
la issue #4.

La nueva plantilla `listaUsuarios.html` reutiliza el fragmento `head` y los
estilos Bootstrap del proyecto. Muestra el título «Listado de usuarios
registrados» y una tabla con las columnas «Identificador» y «Correo electrónico».
Cada fila se genera con `th:each`, y los valores se muestran con `th:text`.
Se presentan únicamente el identificador y el correo; los demás campos del DTO,
incluida la contraseña, no se incorporan a la página. Si la colección está vacía,
se mantiene la cabecera de la tabla y aparece «No hay usuarios registrados.».

`ListadoUsuariosWebTest` añade dos pruebas de integración con MockMvc, el
servicio real y la base de datos H2, limpiada antes de cada prueba mediante
`clean-db.sql`:

- `registradosSinSesionMuestraIdentificadoresYCorreos` registra dos usuarios y
  consulta `/registrados` sin sesión. Comprueba HTTP 200, la vista, las cabeceras,
  las dos filas y la asociación de cada identificador con su correo, sin depender
  del orden. También comprueba que no aparecen contraseñas ni el mensaje de
  listado vacío.
- `registradosSinUsuariosMuestraListadoVacio` comprueba HTTP 200, la vista, la
  ausencia de filas y el mensaje cuando no hay usuarios.

Comando de validación del incremento y del servicio del primer commit:

```bash
./mvnw -Dtest=UsuarioServiceTest,ListadoUsuariosWebTest test
```

Resultado: **10 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`): ocho del servicio y dos de la nueva página.

Las comprobaciones de contenido utilizan cadenas y expresiones regulares sobre
el HTML renderizado. La primera ejecución con XPath falló porque el fragmento
`head` genera HTML con elementos vacíos como `<link>` sin cierre XML; se
ajustaron las pruebas al formato HTML del proyecto y se repitió la validación
con el resultado anterior.

La integración del menú de visitante o usuario queda pendiente del tercer
incremento.

### Tercer incremento: integración del menú y cierre de la funcionalidad

`UsuarioController.listadoUsuarios` consulta la sesión mediante
`ManagerUserSession.usuarioLogeado()` y recupera el DTO del usuario autenticado
con `UsuarioService.findById`. Lo incorpora al modelo como `usuario`, siguiendo
el patrón de «Acerca de». Si no hay sesión o su identificador corresponde a un
usuario que ya no existe, el atributo queda a nulo y se muestra el menú de
visitante. La consulta del listado completo sigue disponible en ambos casos.

La plantilla `listaUsuarios.html` selecciona el fragmento común según ese
atributo:

```html
<nav th:replace="${usuario != null} ? ~{fragments :: menuUsuario(${usuario})} : ~{fragments :: menuVisitante}"></nav>
```

El menú de visitante ofrece «ToDoList», «Login» y «Registro». El menú autenticado
muestra «ToDoList», «Tareas» y el desplegable con el nombre del usuario, «Cuenta»
y «Cerrar sesión». Se añade también el fragmento `javascript` para cargar jQuery,
Popper y Bootstrap, necesarios para el desplegable.

Se ha ampliado `ListadoUsuariosWebTest` con las siguientes comprobaciones:

- La prueba existente sin sesión verifica los enlaces del menú de visitante y
  la ausencia de logout, además del contenido de la tabla.
- `registradosConSesionMuestraMenuDelUsuarioYListadoCompleto` utiliza una sesión
  HTTP con el identificador de un usuario registrado. Comprueba su nombre, los
  enlaces a sus tareas y logout, «Cuenta», la ausencia de login y registro y la
  inclusión de Bootstrap. También verifica que el listado contiene tanto al
  usuario autenticado como a otro usuario registrado.
- `registradosConUsuarioDeSesionInexistenteMuestraMenuVisitante` verifica que una
  sesión con un identificador inexistente mantiene HTTP 200 y muestra el menú
  de visitante y el listado vacío.

Validación final de los tres incrementos mediante la suite completa:

```bash
./mvnw test
```

Resultado: **44 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`), incluidas las cuatro pruebas web del listado. También se
ha comprobado el formato del diff mediante `git diff --check`, sin incidencias.

Con este incremento se completa la funcionalidad de la issue #4: `/registrados`
muestra los identificadores y correos de todos los usuarios, tanto con sesión
como sin ella, con el menú común correspondiente y un mensaje si no hay datos.

## 004 Página de descripción de un usuario

La [issue #5](https://github.com/isfs2-ua/mads-todolist/issues/5) solicita un
enlace desde el listado de usuarios a su descripción en `/registrados/{id}`,
con todos sus datos salvo la contraseña.

### Primer incremento: consulta y página de descripción

Se ha añadido `UsuarioController.descripcionUsuario`, que atiende GET
`/registrados/{id}` y reutiliza `UsuarioService.findById()` para recuperar el
usuario solicitado. Si existe, lo añade al modelo como `usuarioDescripcion` y
devuelve la vista `descripcionUsuario`. Este atributo identifica al usuario
consultado y permite distinguirlo del usuario de sesión cuando se incorpore el
menú en el tercer incremento.

Si el identificador no corresponde a ningún usuario, se lanza la nueva
`UsuarioNotFoundException`, anotada con `@ResponseStatus`: devuelve HTTP 404
con el motivo «Usuario no encontrado», siguiendo el patrón de las excepciones
web existentes del proyecto.

La plantilla `descripcionUsuario.html` reutiliza el fragmento `head` y Bootstrap.
Muestra identificador, correo electrónico, nombre y fecha de nacimiento mediante
una lista de descripción (`dl`, `dt`, `dd`) y `th:text`. La fecha se presenta en
formato `dd/MM/yyyy`. Para nombre vacío o ausente y fecha ausente se muestra
«Sin especificar», ya que ambos campos son opcionales en el registro. La
contraseña no se incorpora al HTML. La consulta es accesible sin sesión.

`DescripcionUsuarioWebTest` incorpora tres pruebas de integración con MockMvc,
el servicio real y H2; `clean-db.sql` limpia los datos antes de cada prueba:

- `descripcionSinSesionMuestraDatosSinPassword` comprueba HTTP 200, la vista,
  los cuatro datos y la ausencia de la contraseña y de su etiqueta.
- `descripcionSinDatosOpcionalesMuestraSinEspecificar` comprueba que la página
  funciona para usuarios registrados sin nombre ni fecha de nacimiento.
- `descripcionUsuarioInexistenteDevuelve404` comprueba HTTP 404 y su motivo.

Comando de validación del incremento junto con el listado y el servicio:

```bash
./mvnw -Dtest=DescripcionUsuarioWebTest,ListadoUsuariosWebTest,UsuarioServiceTest test
```

Resultado: **15 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`): tres de la descripción, cuatro del listado y ocho del
servicio. `git diff --check` también pasa sin incidencias.

Los enlaces desde el listado y de vuelta a él corresponden al segundo
incremento. El menú según la sesión se incorporará en el tercero.

### Segundo incremento: navegación entre listado y descripción

Se ha añadido la columna «Acción» a `listaUsuarios.html`. Cada fila ofrece el
enlace «Ver descripción», generado con
`th:href="@{/registrados/{id}(id=${usuario.id})}"`, que apunta a la ficha del
usuario de esa fila. Se mantienen las columnas de identificador y correo y el
mensaje del listado vacío.

En `descripcionUsuario.html` se incorpora «Volver al listado» con destino
`/registrados`, tanto para usuarios con todos sus datos como para los que no
han indicado los campos opcionales. Ambos enlaces reutilizan estilos Bootstrap.
No es necesario modificar los controladores ni el servicio para este incremento.

Se han ampliado las pruebas existentes:

- Las pruebas del listado con y sin sesión comprueban que cada enlace «Ver
  descripción» está en la fila del identificador y correo correspondientes.
  La comprobación de las dos filas contempla ahora la tercera columna, y se
  verifica su cabecera «Acción». Se utiliza un método auxiliar para comprobar
  la asociación entre datos y enlace sin depender del orden de las filas.
- Las pruebas de descripción con datos completos y con datos opcionales
  ausentes verifican «Volver al listado» y su destino `/registrados`.
- Se mantienen las comprobaciones del listado vacío, la ausencia de contraseñas
  y HTTP 404 para usuarios inexistentes.

Comando de validación del incremento junto con el servicio:

```bash
./mvnw -Dtest=DescripcionUsuarioWebTest,ListadoUsuariosWebTest,UsuarioServiceTest test
```

Resultado: **15 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

El menú de la ficha según la sesión y la validación de la suite completa quedan
pendientes del tercer incremento.

### Tercer incremento: menú de la descripción y validación final

`UsuarioController.descripcionUsuario` recupera el usuario autenticado mediante
`ManagerUserSession.usuarioLogeado()` y `UsuarioService.findById()` y lo añade
al modelo como `usuario`. El atributo `usuarioDescripcion` sigue conteniendo
al usuario solicitado en la URL. Así, al consultar la ficha de otra persona,
el nombre y los enlaces de tareas y logout del menú corresponden a quien ha
iniciado sesión, mientras que la ficha muestra los datos de la persona consultada.

`descripcionUsuario.html` selecciona el fragmento de menú común de la misma
forma que el listado y «Acerca de»:

```html
<nav th:replace="${usuario != null} ? ~{fragments :: menuUsuario(${usuario})} : ~{fragments :: menuVisitante}"></nav>
```

Sin sesión, o si el usuario de la sesión ya no existe, se muestra el menú de
visitante. Se añade el fragmento `javascript` para cargar jQuery, Popper y
Bootstrap, necesarios para el desplegable del menú autenticado. La ficha
mantiene sus datos, el enlace de vuelta al listado y HTTP 404 si el usuario
consultado no existe.

Se ha ampliado `DescripcionUsuarioWebTest`:

- La prueba sin sesión comprueba los enlaces de «ToDoList», «Login» y «Registro»
  y la ausencia de logout, además de los datos y la ausencia de contraseña.
- `descripcionDeOtroUsuarioMuestraMenuDelUsuarioLogeado` registra dos usuarios
  y consulta la ficha del segundo con la sesión del primero. Comprueba que el
  menú muestra el nombre, las tareas y el logout del primero, y que la ficha
  muestra los cuatro datos del segundo. También verifica «Cuenta», Bootstrap,
  el enlace de vuelta y la ausencia de ambas contraseñas y de los enlaces de
  login y registro.
- `descripcionConUsuarioDeSesionInexistenteMuestraMenuVisitante` comprueba HTTP
  200, el menú de visitante y los datos de la ficha con una sesión cuyo
  identificador ya no corresponde a ningún usuario.

Validación final de los tres incrementos con la suite completa:

```bash
./mvnw test
```

Resultado: **49 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`), incluidas las cinco pruebas web de la descripción.
`git diff --check` también pasa sin incidencias.

Con este incremento se completa la funcionalidad de la issue #5: cada usuario
del listado tiene un enlace a `/registrados/{id}`, su ficha muestra todos sus
datos salvo la contraseña y permite volver al listado, con el menú
correspondiente a la sesión.

## 005 Usuario administrador

La [issue #6](https://github.com/isfs2-ua/mads-todolist/issues/6) solicita
permitir el registro de un único administrador y dirigirlo al listado tras
autenticarse.

La implementación del modelo, el formulario y el login está incluida en el
commit `c536f30`, «Añadido rol de administrador único en el servicio de registro».
Las siguientes secciones describen las partes de ese mismo cambio.

### Rol de administrador y regla de alta

`Usuario` y `UsuarioData` incorporan la propiedad `administrador`, consultada
mediante `isAdministrador()`. Los usuarios normales tienen el valor lógico
`false` por defecto. `UsuarioRepository.existsByAdministradorTrue()` permite
consultar si existe un administrador, y `UsuarioService.existeAdministrador()`
expone esa consulta mediante una transacción de lectura.

El servicio de registro rechaza el alta de otro administrador con el mensaje
«Ya existe un usuario administrador». La comprobación se realiza en el servicio,
por lo que también cubre llamadas que no pasan por el formulario. Se siguen
permitiendo las altas de usuarios normales después de registrar al administrador.

En H2, la columna `administrador` tiene una restricción única: guarda `TRUE`
para el administrador y `NULL` para usuarios normales. El getter y el setter
traducen esa representación al booleano de la aplicación. Los múltiples valores
nulos permiten usuarios normales ilimitados; el valor verdadero sólo puede
aparecer una vez. Esta restricción también impide que dos altas simultáneas
creen dos administradores. El servicio traduce el conflicto del INSERT a la
misma excepción de negocio.

Las pruebas verifican la persistencia del rol, la consulta de existencia, las
altas normales antes y después del administrador y el rechazo del segundo
administrador sin guardar sus datos. Una prueba de repositorio intenta guardar
dos administradores directamente y comprueba la restricción de la base de datos.

Validación: `./mvnw -Dtest=UsuarioServiceTest,UsuarioTest test`.
Resultado: **17 pruebas, 0 fallos, 0 errores y 0 omitidas**, `BUILD SUCCESS`.
`git diff --check` también pasa sin incidencias.

### Formulario de registro

`RegistroData` incorpora el booleano `administrador`, cuyo valor por defecto
es `false`. `formRegistro.html` enlaza el checkbox «Registrarse como
administrador» con esa propiedad y lo muestra sólo cuando no existe uno:

```html
<div class="form-check mb-3" th:if="${!existeAdministrador}">
    <input id="administrador" class="form-check-input" type="checkbox"
           th:field="*{administrador}"/>
    <label class="form-check-label" for="administrador">Registrarse como administrador</label>
</div>
```

`LoginController` consulta `UsuarioService.existeAdministrador()` y añade el
resultado al modelo al abrir y procesar el formulario. Así, la disponibilidad
del checkbox también se respeta cuando se vuelve a mostrar la página por
errores de validación o por un correo ya registrado. Si todavía no existe un
administrador, un error de validación conserva la selección realizada.

El controller copia el rol solicitado a `UsuarioData` antes de llamar a
`registrar()`. Si alguien envía un POST directo solicitando ser un segundo
administrador, el servicio lo rechaza. El controller captura
`UsuarioServiceException`, muestra «Ya existe un usuario administrador» y
actualiza la disponibilidad del checkbox. Esta comprobación también cubre el
caso de que otro usuario se registre como administrador después de abrir el
formulario. En la implementación inicial, un registro correcto redirigía a
`/login`, sin iniciar sesión. La mejora de sesión automática documentada al
final de este archivo sustituye ese comportamiento.

### Login y ejemplo de uso

Tras comprobar las credenciales, `LoginController` guarda el identificador del
usuario en sesión mediante `ManagerUserSession.logearUsuario()`. Consulta el
rol del usuario recuperado de la base de datos: si `isAdministrador()` devuelve
`true`, redirige a `/registrados`; para un usuario normal mantiene la
redirección a `/usuarios/{id}/tareas`. El rol no procede del formulario de login.

Por ejemplo, el primer usuario marca «Registrarse como administrador» y completa
el alta. En el flujo inicial, la aplicación lo llevaba al login y, tras
introducir sus credenciales, accedía al listado de usuarios registrados.
Con la mejora posterior, accede directamente al listado tras el alta.
Desde ese momento, el formulario
de registro deja de ofrecer el checkbox, pero permite seguir dando de alta
usuarios normales. Una contraseña incorrecta muestra el error de login y no
abre una nueva sesión.

### Pruebas web y validación final

`AdministradorWebTest` utiliza MockMvc, el servicio real y H2, con limpieza
mediante `clean-db.sql`. Sus ocho pruebas comprueban:

- La aparición del checkbox y su desaparición tras registrar al administrador.
- El alta normal cuando ya existe un administrador.
- El rechazo de un POST directo para crear un segundo administrador, sin
  guardar al usuario solicitado.
- La conservación del checkbox seleccionado tras un error de validación
  cuando todavía no existe administrador.
- La ausencia del checkbox en respuestas con errores de validación o correo
  repetido cuando ya existe administrador.
- La redirección del administrador al listado, el identificador guardado en
  sesión y la carga de `/registrados` con esa sesión.
- La redirección del usuario normal a sus tareas y su identificador en sesión.
- El rechazo de una contraseña incorrecta del administrador sin abrir sesión.

La suite completa se ha vuelto a ejecutar sobre el commit `c536f30`:

```bash
./mvnw test
```

Resultado: **60 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

Con este cambio se completa la funcionalidad de la issue #6. La restricción
de acceso al listado y a las fichas corresponde a la siguiente historia,
«006 Protección de usuarios» (issue #7).

## 006 Protección del listado y la descripción de usuario

La [issue #7](https://github.com/isfs2-ua/mads-todolist/issues/7) solicita
restringir ambas páginas al administrador y devolver HTTP 401 («No autorizado»)
con un mensaje de permisos a quienes intenten acceder sin autorización.

### Primer incremento: protección del listado

`UsuarioController.listadoUsuarios` recupera al usuario de la sesión y comprueba
que existe y que `isAdministrador()` es verdadero antes de consultar el listado.
Si no se cumple, lanza `UsuarioNoAdministradorException`. Esta excepción sigue
el patrón de las páginas de tareas mediante `@ResponseStatus`: devuelve HTTP
401 con el motivo «No autorizado: se necesita permiso de administrador».

`ListadoUsuariosWebTest` adapta las pruebas al nuevo requisito. Comprueba el
rechazo sin sesión, con un usuario normal y con un identificador de sesión
inexistente. El caso autorizado utiliza un administrador y conserva las
comprobaciones del menú, las columnas y las filas con enlaces a las fichas.
El listado vacío deja de ser un caso accesible: el administrador que permite
entrar necesariamente forma parte de los usuarios registrados.

Validación: `./mvnw test`.
Resultado: **60 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

Este incremento protege únicamente `/registrados`. La protección de
`/registrados/{id}` queda pendiente del siguiente paso.

### Segundo incremento: protección de la descripción

`UsuarioController` extrae la comprobación de sesión y rol al método privado
`comprobarAdministrador()`, reutilizado por el listado y la descripción. Devuelve
el DTO del administrador para incorporarlo al menú y mantiene HTTP 401 con el
mismo motivo cuando falta autorización.

La descripción comprueba los permisos antes de buscar la ficha solicitada.
Así, sin permisos siempre se devuelve HTTP 401, incluso si el identificador de
la URL no existe. Un administrador sigue recibiendo HTTP 404 («Usuario no
encontrado») cuando consulta una ficha inexistente.

`DescripcionUsuarioWebTest` adapta los casos de datos completos, campos
opcionales, menú y ficha inexistente para utilizar una sesión de administrador.
Comprueba también el rechazo con una sesión de usuario inexistente. Dos nuevas
pruebas verifican el acceso sin sesión y con un usuario normal: este último
no puede consultar su propia ficha, la de otro usuario ni un identificador
inexistente. Se mantienen las comprobaciones de datos y ausencia de contraseñas
en las respuestas autorizadas.

Validación: `./mvnw test`.
Resultado: **62 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

Con este incremento, tanto `/registrados` como `/registrados/{id}` quedan
protegidos en el servidor mediante el rol de administrador.

### Página de errores personalizada

Se añade `error.html` para sustituir la pantalla predeterminada de Spring Boot
en las respuestas HTML de error. Reutiliza el menú de visitante y los estilos
Bootstrap locales, con una tarjeta centrada que muestra el código HTTP, un
título en español, el motivo y enlaces al login y a «Acerca de ToDoList».

`PaginaErrorResolver` implementa `ErrorViewResolver` y selecciona esta plantilla
manteniendo el estado HTTP original. Para errores 4xx recupera el motivo enviado
por el servlet, como «No autorizado: se necesita permiso de administrador».
Cuando no hay motivo utiliza un mensaje alternativo; para 404 indica que la
página o el recurso no existe. Los errores internos muestran un mensaje genérico.
El texto se presenta con `th:text`, sin interpretar HTML incluido en el motivo.

`PaginaErrorWebTest` arranca Tomcat en un puerto aleatorio y solicita HTML para
verificar el flujo real de errores: ambas rutas protegidas devuelven la nueva
página con HTTP 401 y su motivo; una ruta inexistente conserva HTTP 404 y muestra
el mensaje correspondiente. Otra prueba comprueba que el resolver sustituye
los detalles de un fallo interno por el mensaje genérico para HTTP 500.

Validación: `./mvnw test`.
Resultado: **65 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.
Estas pruebas necesitan permiso para abrir el puerto local de Tomcat.

## 007 Administrador: bloqueo del acceso a usuarios

La [issue #8](https://github.com/isfs2-ua/mads-todolist/issues/8) solicita
añadir botones para bloquear o habilitar usuarios desde el listado y mostrar
un error de login cuando una cuenta bloqueada intente acceder.

### Primer incremento: estado de bloqueo y servicio

`Usuario` y `UsuarioData` incorporan la propiedad booleana `bloqueado`, con
valor `false` por defecto. ModelMapper traslada ese estado al DTO en las consultas
existentes. Los usuarios nuevos se crean habilitados.

`UsuarioService.cambiarBloqueo(usuarioId, bloqueado)` busca la entidad y cambia
su estado dentro de una transacción. JPA persiste el cambio al completar la
transacción; el método devuelve el DTO actualizado. Si el usuario no existe,
lanza `UsuarioServiceException` con el mensaje «El usuario no existe».

Dos pruebas de servicio verifican el estado inicial, el bloqueo persistido en
las consultas por identificador, correo y listado, y la habilitación posterior.
Comprueban que otro usuario conserva su estado y que el cambio no altera los
datos de la cuenta. También cubren ambos cambios sobre un identificador
inexistente, sin crear usuarios.

Validación: `./mvnw -Dtest=UsuarioServiceTest,UsuarioTest test`.
Resultado: **19 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

Este incremento prepara la persistencia y el servicio. Los endpoints con
comprobación de administrador, los botones del listado y el rechazo del login
de una cuenta bloqueada quedan para los siguientes pasos.

### Segundo incremento: botones y endpoint de bloqueo

El listado incorpora un formulario POST por usuario normal. Envía a
`/registrados/{id}/bloqueo` el parámetro booleano `bloqueado`, con el estado
contrario al actual: «Bloquear» si está habilitado y «Habilitar» si está bloqueado.
Los botones utilizan los estilos Bootstrap y comparten la celda con el enlace
a la descripción.

`UsuarioController.cambiarBloqueo` reutiliza `comprobarAdministrador()` antes de
consultar la cuenta. Sin permisos devuelve HTTP 401; si el usuario solicitado
no existe, HTTP 404. Tras llamar al servicio redirige a `/registrados`, donde el
botón refleja el estado actualizado. El administrador no tiene botón de bloqueo:
para evitar perder el único acceso de administración, un POST directo para
bloquearlo también se rechaza con HTTP 400 y «No se puede bloquear al administrador».

`BloqueoUsuariosWebTest` verifica el formulario, el cambio de ambos estados y la
redirección, además de los POST sin sesión, con un usuario normal o con un
identificador de sesión inexistente. En esos casos comprueba que el estado no
cambia, tanto al intentar bloquear como habilitar. También cubre el usuario
inexistente y el intento de bloqueo del administrador. La prueba del listado
adapta su patrón de filas para admitir el formulario junto al enlace existente.

Validación:

```bash
./mvnw -Dtest=BloqueoUsuariosWebTest,ListadoUsuariosWebTest,UsuarioServiceTest test
```

Resultado: **20 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

El rechazo del login de una cuenta bloqueada queda pendiente del siguiente paso.

### Tercer incremento: rechazo del login bloqueado

`UsuarioService.LoginStatus` incorpora `USER_BLOCKED`. El servicio comprueba
primero la existencia del usuario y la contraseña; cuando las credenciales
son correctas, una cuenta bloqueada devuelve ese estado en lugar de `LOGIN_OK`.
Los errores de usuario inexistente o contraseña incorrecta conservan su resultado.

`LoginController` responde a `USER_BLOCKED` mostrando de nuevo `formLogin`, con
HTTP 200 y «Tu cuenta está bloqueada. Contacta con el administrador.». La plantilla
ya dispone de una alerta para el atributo `error`, por lo que se reutiliza sin
modificarla. Este resultado no establece una sesión autenticada ni redirige a
las tareas. Después de habilitar la cuenta, sus credenciales vuelven a permitir
el acceso habitual.

Una prueba de servicio cubre el login antes del bloqueo, durante el bloqueo y
tras habilitar la cuenta, junto con los errores de credenciales. La nueva prueba
de `BloqueoUsuariosWebTest` recorre el flujo con el servicio real: el administrador
bloquea mediante POST, el usuario intenta acceder y recibe el mensaje sin obtener
un identificador autenticado en sesión. Tras habilitarlo, el login redirige a sus
tareas y guarda su identificador en sesión.

Ejemplo: el administrador pulsa «Bloquear» en la fila de un usuario; el botón
pasa a «Habilitar». Si ese usuario intenta iniciar sesión con su contraseña,
ve el mensaje de cuenta bloqueada. Al pulsar «Habilitar», el administrador
restablece la posibilidad de iniciar sesión. El bloqueo afecta a los intentos
de login; las sesiones que ya estaban abiertas no se invalidan en esta historia.

Validación final: `./mvnw test`.
Resultado: **73 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.

Con este incremento se completa la funcionalidad de la issue #8: el administrador
puede bloquear o habilitar usuarios desde el listado y las cuentas bloqueadas
reciben un mensaje explicativo al intentar iniciar sesión.

## Mejora: sesión automática tras el registro

Después de un alta correcta, `LoginController` utiliza el DTO devuelto por
`UsuarioService.registrar()`, que contiene el identificador persistido, e inicia
sesión mediante `ManagerUserSession`. El método privado `iniciarSesion()` se
comparte con el login para mantener el mismo destino: `/registrados` para el
administrador y `/usuarios/{id}/tareas` para el usuario normal. Así, el usuario
puede empezar a utilizar la aplicación sin volver a introducir sus credenciales.

Las pruebas de `AdministradorWebTest` comprueban el identificador guardado en
sesión, la redirección y el acceso inmediato a la página de destino tras registrar
un administrador o un usuario normal. El alta normal se verifica tanto con un
administrador existente como sin él. También se comprueba que un error de
validación, un correo repetido o el intento de crear un segundo administrador
no abren una sesión autenticada.

Validación:

```bash
./mvnw -Dtest=AdministradorWebTest,UsuarioWebTest,BloqueoUsuariosWebTest test
```

Resultado: **17 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**
(`BUILD SUCCESS`). `git diff --check` también pasa sin incidencias.
