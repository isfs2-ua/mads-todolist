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

Este incremento cubre la navegación para visitantes. Quedan pendientes el menú
para usuarios autenticados, la selección del menú según la sesión y su
incorporación a las páginas de tareas; por tanto, la issue #3 sigue incompleta.
