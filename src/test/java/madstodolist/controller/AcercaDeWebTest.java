package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class AcercaDeWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    public void getAboutDevuelveNombreAplicacion() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("ToDoList")));
    }

    @Test
    public void getAboutSinSesionMuestraEnlacesDeNavegacion() throws Exception {
        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/login\">Login</a>")))
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))));
    }

    @Test
    public void getAboutConSesionMuestraMenuDelUsuario() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("menu@ua.es");
        usuario.setNombre("Usuario del menú");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());

        this.mockMvc.perform(get("/about").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a class=\"navbar-brand\" href=\"/about\">ToDoList</a>")))
                .andExpect(content().string(containsString("href=\"/usuarios/" + usuario.getId() + "/tareas\">Tareas</a>")))
                .andExpect(content().string(containsString(">Usuario del menú</button>")))
                .andExpect(content().string(containsString(">Cuenta</span>")))
                .andExpect(content().string(containsString("href=\"/logout\">Cerrar sesión Usuario del menú</a>")))
                .andExpect(content().string(not(containsString("href=\"/login\""))))
                .andExpect(content().string(not(containsString("href=\"/registro\""))));
    }
}
