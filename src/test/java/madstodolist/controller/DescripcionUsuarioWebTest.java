package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class DescripcionUsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    public void descripcionSinSesionMuestraDatosSinPassword() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("descripcion@ua.es");
        usuario.setNombre("Ana García");
        usuario.setPassword("clave-privada-descripcion");
        usuario.setFechaNacimiento(new SimpleDateFormat("dd/MM/yyyy").parse("15/04/1995"));
        usuario = usuarioService.registrar(usuario);

        mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("descripcionUsuario"))
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/login\">Login</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))))
                .andExpect(content().string(containsString("href=\"/registrados\">Volver al listado</a>")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Identificador</dt>\\s*<dd>"
                        + usuario.getId() + "</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Correo electrónico</dt>\\s*"
                        + "<dd>descripcion@ua[.]es</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Nombre</dt>\\s*<dd>Ana García</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Fecha de nacimiento</dt>\\s*"
                        + "<dd>15/04/1995</dd>.*")))
                .andExpect(content().string(not(containsString("clave-privada-descripcion"))))
                .andExpect(content().string(not(containsString("Contraseña"))));
    }

    @Test
    public void descripcionSinDatosOpcionalesMuestraSinEspecificar() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("sin-opcionales@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/registrados\">Volver al listado</a>")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Nombre</dt>\\s*"
                        + "<dd>Sin especificar</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Fecha de nacimiento</dt>\\s*"
                        + "<dd>Sin especificar</dd>.*")));
    }

    @Test
    public void descripcionDeOtroUsuarioMuestraMenuDelUsuarioLogeado() throws Exception {
        UsuarioData usuarioLogeado = new UsuarioData();
        usuarioLogeado.setEmail("sesion@ua.es");
        usuarioLogeado.setNombre("Usuario de sesión");
        usuarioLogeado.setPassword("clave-sesion-privada");
        usuarioLogeado = usuarioService.registrar(usuarioLogeado);

        UsuarioData usuarioConsultado = new UsuarioData();
        usuarioConsultado.setEmail("consultado@ua.es");
        usuarioConsultado.setNombre("Usuario consultado");
        usuarioConsultado.setPassword("clave-consultado-privada");
        usuarioConsultado.setFechaNacimiento(new SimpleDateFormat("dd/MM/yyyy").parse("20/06/1990"));
        usuarioConsultado = usuarioService.registrar(usuarioConsultado);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuarioLogeado.getId());

        mockMvc.perform(get("/registrados/" + usuarioConsultado.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("descripcionUsuario"))
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("href=\"/usuarios/" + usuarioLogeado.getId() + "/tareas\">Tareas</a>")))
                .andExpect(content().string(containsString(">Usuario de sesión</button>")))
                .andExpect(content().string(containsString(">Cuenta</span>")))
                .andExpect(content().string(containsString("href=\"/logout\">Cerrar sesión Usuario de sesión</a>")))
                .andExpect(content().string(not(containsString("href=\"/usuarios/" + usuarioConsultado.getId() + "/tareas\""))))
                .andExpect(content().string(not(containsString(">Usuario consultado</button>"))))
                .andExpect(content().string(not(containsString("href=\"/login\""))))
                .andExpect(content().string(not(containsString("href=\"/registro\""))))
                .andExpect(content().string(containsString("src=\"/js/bootstrap.min.js\"")))
                .andExpect(content().string(containsString("href=\"/registrados\">Volver al listado</a>")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Identificador</dt>\\s*<dd>"
                        + usuarioConsultado.getId() + "</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Correo electrónico</dt>\\s*"
                        + "<dd>consultado@ua[.]es</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Nombre</dt>\\s*"
                        + "<dd>Usuario consultado</dd>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dt>Fecha de nacimiento</dt>\\s*"
                        + "<dd>20/06/1990</dd>.*")))
                .andExpect(content().string(not(containsString("clave-sesion-privada"))))
                .andExpect(content().string(not(containsString("clave-consultado-privada"))));
    }

    @Test
    public void descripcionConUsuarioDeSesionInexistenteMuestraMenuVisitante() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("ficha@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", Long.MAX_VALUE);

        mockMvc.perform(get("/registrados/" + usuario.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/login\">Login</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))))
                .andExpect(content().string(containsString("<dd>ficha@ua.es</dd>")));
    }

    @Test
    public void descripcionUsuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/registrados/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Usuario no encontrado"));
    }
}
