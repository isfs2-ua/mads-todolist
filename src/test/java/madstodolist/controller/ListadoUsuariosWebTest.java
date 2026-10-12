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

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class ListadoUsuariosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    private String patronFilaUsuario(Long id, String email) {
        return "(?s).*<tr>\\s*<td>" + id + "</td>\\s*<td>" + Pattern.quote(email)
                + "</td>\\s*<td>\\s*<a[^>]* href=\"/registrados/" + id
                + "\">Ver descripción</a>\\s*</td>\\s*</tr>.*";
    }

    @Test
    public void registradosSinSesionMuestraIdentificadoresYCorreos() throws Exception {
        UsuarioData primero = new UsuarioData();
        primero.setEmail("primero@ua.es");
        primero.setPassword("clave-primero");
        primero = usuarioService.registrar(primero);

        UsuarioData segundo = new UsuarioData();
        segundo.setEmail("segundo@ua.es");
        segundo.setPassword("clave-segundo");
        segundo = usuarioService.registrar(segundo);

        mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(view().name("listaUsuarios"))
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/login\">Login</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))))
                .andExpect(content().string(containsString("<th scope=\"col\">Identificador</th>")))
                .andExpect(content().string(containsString("<th scope=\"col\">Correo electrónico</th>")))
                .andExpect(content().string(containsString("<th scope=\"col\">Acción</th>")))
                .andExpect(content().string(matchesPattern("(?s).*<tbody>\\s*"
                        + "(<tr>\\s*<td>[^<]+</td>\\s*<td>[^<]+</td>\\s*<td>\\s*"
                        + "<a[^>]*>Ver descripción</a>\\s*</td>\\s*</tr>\\s*){2}"
                        + "</tbody>.*")))
                .andExpect(content().string(matchesPattern(patronFilaUsuario(primero.getId(), primero.getEmail()))))
                .andExpect(content().string(matchesPattern(patronFilaUsuario(segundo.getId(), segundo.getEmail()))))
                .andExpect(content().string(not(containsString("clave-primero"))))
                .andExpect(content().string(not(containsString("clave-segundo"))))
                .andExpect(content().string(not(containsString("No hay usuarios registrados."))));
    }

    @Test
    public void registradosConSesionMuestraMenuDelUsuarioYListadoCompleto() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("menu@ua.es");
        usuario.setNombre("Usuario del menú");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        UsuarioData otro = new UsuarioData();
        otro.setEmail("otro@ua.es");
        otro.setPassword("456");
        otro = usuarioService.registrar(otro);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("listaUsuarios"))
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("href=\"/usuarios/" + usuario.getId() + "/tareas\">Tareas</a>")))
                .andExpect(content().string(containsString(">Usuario del menú</button>")))
                .andExpect(content().string(containsString(">Cuenta</span>")))
                .andExpect(content().string(containsString("href=\"/logout\">Cerrar sesión Usuario del menú</a>")))
                .andExpect(content().string(not(containsString("href=\"/login\""))))
                .andExpect(content().string(not(containsString("href=\"/registro\""))))
                .andExpect(content().string(containsString("src=\"/js/bootstrap.min.js\"")))
                .andExpect(content().string(matchesPattern(patronFilaUsuario(usuario.getId(), usuario.getEmail()))))
                .andExpect(content().string(matchesPattern(patronFilaUsuario(otro.getId(), otro.getEmail()))));
    }

    @Test
    public void registradosConUsuarioDeSesionInexistenteMuestraMenuVisitante() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", Long.MAX_VALUE);

        mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/login\">Login</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))))
                .andExpect(content().string(containsString("No hay usuarios registrados.")));
    }

    @Test
    public void registradosSinUsuariosMuestraListadoVacio() throws Exception {
        mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(view().name("listaUsuarios"))
                .andExpect(content().string(matchesPattern("(?s).*<tbody>\\s*</tbody>.*")))
                .andExpect(content().string(containsString("No hay usuarios registrados.")));
    }
}
