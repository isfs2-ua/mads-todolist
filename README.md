# Aplicación inicial ToDoList

Aplicación ToDoList de la asignatura [MADS](https://cvnet.cpd.ua.es/Guia-Docente/GuiaDocente/Index?wcodest=C203&wcodasi=34037&wlengua=es&scaca=2019-20) usando Spring Boot y plantillas Thymeleaf.

## Enlaces

Enlace al [Tablero Trello](https://trello.com/b/xYyf7yRf/todolist-mads).

## Requisitos

Necesitas tener instalado en tu sistema:

- Java 8

## Tests
Para ejecutar los tests con la base de datos PostgreSQL, arrancamos un contenedor con la base de datos de test:

```
$ docker run -p 5432:5432 --name postgres-test -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads_test postgres:13
```

Y ejecutamos:

```
$ ./mvnw -D spring.profiles.active=postgres test
```

## Ejecución
La base de datos PostgreSQL se ejecuta con un contenedor de Docker. El comando a usar es el siguiente:

```
$ docker run -p 5432:5432 --name postgres-develop -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads postgres:13
```

Para pararlo:

```
docker container stop postgres-develop
```

Puedes ejecutar la aplicación usando el _goal_ `run` del _plugin_ Maven 
de Spring Boot:

```
$ ./mvnw spring-boot:run 
```   

También puedes generar un `jar` y ejecutarlo:

```
$ ./mvnw package
$ java -jar target/mads-todolist-inicial-0.0.1-SNAPSHOT.jar 
```

Si se quiere ejecutar usando el perfil de Postgres, se usa:

```
$ ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

También se puede con un JAR

```
$ ./mvnw package
$ java -Dspring.profiles.active=postgres -jar target/*.jar
```

Una vez lanzada la aplicación puedes abrir un navegador y probar la página de inicio:

- [http://localhost:8080/login](http://localhost:8080/login)
