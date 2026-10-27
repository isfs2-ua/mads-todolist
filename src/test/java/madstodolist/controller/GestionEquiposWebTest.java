package madstodolist.controller;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class GestionEquiposWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    private UsuarioData crearUsuario(boolean administrador) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(administrador ? "admin@ua.es" : "participante@ua.es");
        usuario.setNombre(administrador ? "Administrador" : "Participante");
        usuario.setPassword("123");
        usuario.setAdministrador(administrador);
        return usuarioService.registrar(usuario);
    }

    private MockHttpSession sesion(UsuarioData usuario) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idUsuarioLogeado", usuario.getId());
        return session;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void opcionesDeGestionSoloAparecenParaAdministrador(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        EquipoData equipo = equipoService.crearEquipo("Proyecto");
        String editar = "href=\"/equipos/" + equipo.getId() + "/editar\"";
        String eliminar = "href=\"/equipos/" + equipo.getId() + "/eliminar\"";

        mockMvc.perform(get("/equipos").session(sesion(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string(administrador ? containsString(editar) : not(containsString(editar))))
                .andExpect(content().string(administrador ? containsString(eliminar) : not(containsString(eliminar))))
                .andExpect(content().string(administrador ? containsString("Editar equipo") : not(containsString("Editar equipo"))))
                .andExpect(content().string(administrador ? containsString("Eliminar equipo") : not(containsString("Eliminar equipo"))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void usuarioNormalNoPuedeGestionarEquiposNiMediantePeticionDirecta(boolean equipoInexistente) throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Proyecto");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        Long id = equipoInexistente ? Long.MAX_VALUE : equipo.getId();
        MockHttpSession session = sesion(usuario);
        MockHttpServletRequestBuilder[] peticiones = {
                get("/equipos/" + id + "/editar"),
                post("/equipos/" + id + "/editar").param("nombre", "Cambio no permitido"),
                get("/equipos/" + id + "/eliminar"),
                post("/equipos/" + id + "/eliminar").param("confirmar", "true")
        };
        for (MockHttpServletRequestBuilder peticion : peticiones) {
            mockMvc.perform(peticion.session(session))
                    .andExpect(status().isUnauthorized())
                    .andExpect(status().reason("No autorizado: se necesita permiso de administrador"));
        }
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto");
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void eliminacionSinSesionValidaNoModificaEquipo(boolean usuarioInexistente) throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto");
        MockHttpSession session = new MockHttpSession();
        if (usuarioInexistente) {
            session.setAttribute("idUsuarioLogeado", Long.MAX_VALUE);
        }
        mockMvc.perform(get("/equipos/" + equipo.getId() + "/eliminar").session(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/equipos/" + equipo.getId() + "/eliminar").session(session).param("confirmar", "true"))
                .andExpect(status().isUnauthorized());
        assertThat(equipoService.recuperarEquipo(equipo.getId())).isEqualTo(equipo);
    }

    @Test
    public void confirmacionYCancelacionConservanEquipoYParticipantes() throws Exception {
        UsuarioData admin = crearUsuario(true);
        UsuarioData participante = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Proyecto a conservar");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), participante.getId());
        MockHttpSession session = sesion(admin);

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/eliminar").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("confirmarEliminarEquipo"))
                .andExpect(model().attribute("equipo", equipo))
                .andExpect(model().attribute("usuario", admin))
                .andExpect(content().string(containsString("¿Seguro que quieres eliminar el equipo")))
                .andExpect(content().string(containsString("Proyecto a conservar")))
                .andExpect(content().string(containsString("method=\"post\" action=\"/equipos/" + equipo.getId() + "/eliminar\"")))
                .andExpect(content().string(containsString("name=\"confirmar\" value=\"true\"")))
                .andExpect(content().string(containsString("href=\"/equipos\">Cancelar</a>")));

        // Cancelar sigue el enlace al listado; la consulta de confirmación no borra.
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proyecto a conservar")));
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto a conservar");
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(participante);
        assertThat(equipoService.equiposUsuario(participante.getId())).containsExactly(equipo);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"false"})
    public void eliminarSinConfirmacionExplicitaConservaEquipo(String confirmar) throws Exception {
        UsuarioData admin = crearUsuario(true);
        EquipoData equipo = equipoService.crearEquipo("Proyecto");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), admin.getId());
        MockHttpServletRequestBuilder peticion = post("/equipos/" + equipo.getId() + "/eliminar").session(sesion(admin));
        if (confirmar != null) {
            peticion.param("confirmar", confirmar);
        }
        mockMvc.perform(peticion)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attributeCount(0));
        assertThat(equipoService.recuperarEquipo(equipo.getId())).isEqualTo(equipo);
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(admin);
    }

    @Test
    public void confirmarEliminacionBorraEquipoYPertenenciasPeroConservaCuentas() throws Exception {
        UsuarioData admin = crearUsuario(true);
        UsuarioData participante = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Equipo a eliminar");
        EquipoData otro = equipoService.crearEquipo("Equipo conservado");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), admin.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), participante.getId());
        equipoService.añadirUsuarioAEquipo(otro.getId(), participante.getId());
        MockHttpSession session = sesion(admin);

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/eliminar").session(session)
                        .param("confirmar", "true").param("id", otro.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Equipo eliminado correctamente"));
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(otro);
        assertThat(usuarioService.findById(admin.getId())).isEqualTo(admin);
        assertThat(usuarioService.findById(participante.getId())).isEqualTo(participante);
        assertThat(equipoService.equiposUsuario(admin.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(participante.getId())).containsExactly(otro);
        assertThat(equipoService.usuariosEquipo(otro.getId())).containsExactly(participante);

        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Equipo eliminado correctamente")))
                .andExpect(content().string(not(containsString("Equipo a eliminar"))));
        for (String pagina : new String[]{"miembros", "editar", "eliminar"}) {
            mockMvc.perform(get("/equipos/" + equipo.getId() + "/" + pagina).session(session))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    public void eliminarEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData admin = crearUsuario(true);
        mockMvc.perform(get("/equipos/" + Long.MAX_VALUE + "/eliminar").session(sesion(admin)))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));
        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/eliminar").session(sesion(admin)).param("confirmar", "true"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));
    }
}
