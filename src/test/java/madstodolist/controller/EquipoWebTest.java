package madstodolist.controller;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    private UsuarioData crearUsuario(boolean administrador) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("equipos@ua.es");
        usuario.setNombre("Usuario de equipos");
        usuario.setPassword("clave-equipos");
        usuario.setAdministrador(administrador);
        return usuarioService.registrar(usuario);
    }

    private MockHttpSession sesion(Long idUsuario) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", idUsuario);
        return session;
    }

    @Test
    public void listadoMuestraTodosLosEquiposOrdenadosYMenu() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData zeta = equipoService.crearEquipo("Zeta");
        EquipoData alfa = equipoService.crearEquipo("Alfa");

        mockMvc.perform(get("/equipos").session(sesion(usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("listaEquipos"))
                .andExpect(model().attribute("usuario", usuario))
                .andExpect(model().attribute("equipos", contains(alfa, zeta)))
                .andExpect(content().string(matchesPattern("(?s).*<td>" + alfa.getId()
                        + "</td>\\s*<td>Alfa</td>.*<td>" + zeta.getId() + "</td>\\s*<td>Zeta</td>.*")))
                .andExpect(content().string(containsString("href=\"/equipos\">Equipos</a>")))
                .andExpect(content().string(containsString("Cerrar sesión Usuario de equipos")))
                .andExpect(content().string(not(containsString("No hay equipos registrados."))))
                .andExpect(content().string(not(containsString("clave-equipos"))));
    }

    @Test
    public void listadoVacioMuestraMensaje() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(get("/equipos").session(sesion(usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("listaEquipos"))
                .andExpect(model().attribute("equipos", empty()))
                .andExpect(content().string(containsString("No hay equipos registrados.")));
    }

    @Test
    public void administradorPuedeConsultarEquipos() throws Exception {
        UsuarioData administrador = crearUsuario(true);

        mockMvc.perform(get("/equipos").session(sesion(administrador.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("listaEquipos"));
    }

    @Test
    public void listadoSinSesionDevuelveNoAutorizado() throws Exception {
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @Test
    public void listadoConUsuarioInexistenteDevuelveNoAutorizado() throws Exception {
        mockMvc.perform(get("/equipos").session(sesion(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }
}
