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
public class AdministradorWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    private UsuarioData registrarAdministrador() {
        UsuarioData administrador = new UsuarioData();
        administrador.setEmail("admin@ua.es");
        administrador.setPassword("123");
        administrador.setAdministrador(true);
        return usuarioService.registrar(administrador);
    }

    @Test
    public void loginAdministradorAbreSesionYRedirigeAlListado() throws Exception {
        UsuarioData administrador = registrarAdministrador();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session)
                        .param("eMail", "admin@ua.es")
                        .param("password", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));
        assertThat(session.getAttribute("idUsuarioLogeado")).isEqualTo(administrador.getId());

        mockMvc.perform(get("/registrados").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("listaUsuarios"))
                .andExpect(content().string(containsString("admin@ua.es")));
    }

    @Test
    public void loginNormalAbreSesionYRedirigeASusTareas() throws Exception {
        registrarAdministrador();
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("normal@ua.es");
        usuario.setPassword("456");
        usuario = usuarioService.registrar(usuario);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").session(session)
                        .param("eMail", "normal@ua.es")
                        .param("password", "456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/" + usuario.getId() + "/tareas"));
        assertThat(session.getAttribute("idUsuarioLogeado")).isEqualTo(usuario.getId());
    }

    @Test
    public void loginAdministradorConClaveIncorrectaNoAbreSesion() throws Exception {
        registrarAdministrador();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session)
                        .param("eMail", "admin@ua.es")
                        .param("password", "incorrecta"))
                .andExpect(status().isOk())
                .andExpect(view().name("formLogin"))
                .andExpect(content().string(containsString("Contraseña incorrecta")));
        assertThat(session.getAttribute("idUsuarioLogeado")).isNull();
    }

    @Test
    public void formularioOfreceAdministradorHastaSuRegistro() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"checkbox\"")))
                .andExpect(content().string(containsString("name=\"administrador\"")));

        mockMvc.perform(post("/registro")
                        .param("email", "admin@ua.es")
                        .param("password", "123")
                        .param("nombre", "Administrador")
                        .param("administrador", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(usuarioService.findByEmail("admin@ua.es").isAdministrador()).isTrue();
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"administrador\""))));
    }

    @Test
    public void altaNormalSinCheckboxSigueDisponibleConAdministrador() throws Exception {
        registrarAdministrador();
        mockMvc.perform(post("/registro")
                        .param("email", "normal@ua.es")
                        .param("password", "456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(usuarioService.findByEmail("normal@ua.es").isAdministrador()).isFalse();
        assertThat(usuarioService.findAll()).hasSize(2);
    }

    @Test
    public void altaDirectaDeSegundoAdministradorDevuelveErrorSinCrearlo() throws Exception {
        registrarAdministrador();
        mockMvc.perform(post("/registro")
                        .param("email", "otro.admin@ua.es")
                        .param("password", "456")
                        .param("administrador", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(content().string(containsString("Ya existe un usuario administrador")))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));
        assertThat(usuarioService.findByEmail("otro.admin@ua.es")).isNull();
        assertThat(usuarioService.findAll()).hasSize(1);
    }

    @Test
    public void errorDeValidacionMantieneCheckboxCuandoNoHayAdministrador() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("email", "correo-invalido")
                        .param("password", "123")
                        .param("administrador", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attributeHasFieldErrors("registroData", "eMail"))
                .andExpect(content().string(containsString("name=\"administrador\"")))
                .andExpect(content().string(containsString("checked=\"checked\"")));
        assertThat(usuarioService.findAll()).isEmpty();
    }

    @Test
    public void erroresDeValidacionYCorreoRepetidoOcultanCheckboxSiYaHayAdministrador() throws Exception {
        registrarAdministrador();
        mockMvc.perform(post("/registro")
                        .param("email", "correo-invalido")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registroData", "eMail"))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));

        mockMvc.perform(post("/registro")
                        .param("email", "admin@ua.es")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El usuario admin@ua.es ya existe")))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));
        assertThat(usuarioService.findAll()).hasSize(1);
    }
}
