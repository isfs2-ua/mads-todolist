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

Queda pendiente incorporar el menú a las páginas de tareas; la issue #3 todavía
no está completada.
