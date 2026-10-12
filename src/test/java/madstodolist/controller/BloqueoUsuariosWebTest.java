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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class BloqueoUsuariosWebTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioService usuarioService;

    private UsuarioData registrar(String email, boolean administrador) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("123");
        usuario.setAdministrador(administrador);
        return usuarioService.registrar(usuario);
    }

    private MockHttpSession sesion(Long id) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", id);
        return session;
    }

    @Test
    public void loginBloqueadoMuestraErrorSinAbrirSesionYFuncionaTrasHabilitarlo() throws Exception {
        UsuarioData admin = registrar("admin@ua.es", true);
        UsuarioData normal = registrar("normal@ua.es", false);
        MockHttpSession sessionAdmin = sesion(admin.getId());
        MockHttpSession sessionUsuario = new MockHttpSession();
        String ruta = "/registrados/" + normal.getId() + "/bloqueo";

        mockMvc.perform(post(ruta).session(sessionAdmin).param("bloqueado", "true"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login").session(sessionUsuario)
                        .param("eMail", normal.getEmail()).param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("formLogin"))
                .andExpect(content().string(containsString("Tu cuenta está bloqueada. Contacta con el administrador.")));
        assertThat(sessionUsuario.getAttribute("idUsuarioLogeado")).isNull();

        mockMvc.perform(post(ruta).session(sessionAdmin).param("bloqueado", "false"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login").session(sessionUsuario)
                        .param("eMail", normal.getEmail()).param("password", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/" + normal.getId() + "/tareas"));
        assertThat(sessionUsuario.getAttribute("idUsuarioLogeado")).isEqualTo(normal.getId());
    }

    @Test
    public void administradorBloqueaYHabilitaDesdeElListado() throws Exception {
        UsuarioData admin = registrar("admin@ua.es", true);
        UsuarioData normal = registrar("normal@ua.es", false);
        MockHttpSession session = sesion(admin.getId());
        String ruta = "/registrados/" + normal.getId() + "/bloqueo";

        mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"" + ruta + "\"")))
                .andExpect(content().string(containsString("name=\"bloqueado\" value=\"true\"")))
                .andExpect(content().string(containsString(">Bloquear</button>")))
                .andExpect(content().string(not(containsString("action=\"/registrados/" + admin.getId() + "/bloqueo\""))));

        mockMvc.perform(post(ruta).session(session).param("bloqueado", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/registrados"));
        assertThat(usuarioService.findById(normal.getId()).isBloqueado()).isTrue();
        mockMvc.perform(get("/registrados").session(session))
                .andExpect(content().string(containsString("name=\"bloqueado\" value=\"false\"")))
                .andExpect(content().string(containsString(">Habilitar</button>")))
                .andExpect(content().string(not(containsString(">Bloquear</button>"))));

        mockMvc.perform(post(ruta).session(session).param("bloqueado", "false"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/registrados"));
        assertThat(usuarioService.findById(normal.getId()).isBloqueado()).isFalse();
    }

    @Test
    public void cambiosSinSesionConUsuarioNormalOSesionInexistenteSeRechazan() throws Exception {
        UsuarioData normal = registrar("normal@ua.es", false);
        String ruta = "/registrados/" + normal.getId() + "/bloqueo";
        for (String valor : new String[]{"true", "false"}) {
            boolean estadoInicial = !Boolean.parseBoolean(valor);
            usuarioService.cambiarBloqueo(normal.getId(), estadoInicial);
            mockMvc.perform(post(ruta).param("bloqueado", valor))
                    .andExpect(status().isUnauthorized());
            assertThat(usuarioService.findById(normal.getId()).isBloqueado()).isEqualTo(estadoInicial);
            for (Long idSesion : new Long[]{normal.getId(), Long.MAX_VALUE}) {
                mockMvc.perform(post(ruta).session(sesion(idSesion)).param("bloqueado", valor))
                        .andExpect(status().isUnauthorized())
                        .andExpect(status().reason("No autorizado: se necesita permiso de administrador"));
                assertThat(usuarioService.findById(normal.getId()).isBloqueado()).isEqualTo(estadoInicial);
            }
        }
    }

    @Test
    public void administradorRecibe404AlCambiarUsuarioInexistente() throws Exception {
        UsuarioData admin = registrar("admin@ua.es", true);
        for (String valor : new String[]{"true", "false"}) {
            mockMvc.perform(post("/registrados/" + Long.MAX_VALUE + "/bloqueo")
                            .session(sesion(admin.getId())).param("bloqueado", valor))
                    .andExpect(status().isNotFound()).andExpect(status().reason("Usuario no encontrado"));
        }
    }

    @Test
    public void peticionDirectaNoPuedeBloquearAlAdministrador() throws Exception {
        UsuarioData admin = registrar("admin@ua.es", true);
        mockMvc.perform(post("/registrados/" + admin.getId() + "/bloqueo")
                        .session(sesion(admin.getId())).param("bloqueado", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("No se puede bloquear al administrador"));
        assertThat(usuarioService.findById(admin.getId()).isBloqueado()).isFalse();
    }
}
