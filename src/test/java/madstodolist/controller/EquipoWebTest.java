package madstodolist.controller;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    public void formularioNuevoEquipoMuestraNombreYEnlaces() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(get("/equipos/nuevo").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("formNuevoEquipo"))
                .andExpect(model().attribute("usuario", usuario))
                .andExpect(model().attribute("equipoData", hasProperty("nombre", nullValue())))
                .andExpect(content().string(containsString("method=\"post\" action=\"/equipos/nuevo\"")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("for=\"nombre\"")))
                .andExpect(content().string(containsString(">Crear equipo</button>")))
                .andExpect(content().string(containsString("href=\"/equipos\">Cancelar</a>")))
                .andExpect(content().string(containsString("Cerrar sesión Usuario de equipos")));

        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/equipos/nuevo\">Nuevo equipo</a>")));
    }

    @Test
    public void formularioNuevoEquipoPermiteAdministrador() throws Exception {
        UsuarioData administrador = crearUsuario(true);

        mockMvc.perform(get("/equipos/nuevo").session(sesion(administrador.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("formNuevoEquipo"));
    }

    @Test
    public void formularioNuevoEquipoSinSesionDevuelveNoAutorizado() throws Exception {
        mockMvc.perform(get("/equipos/nuevo"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @Test
    public void formularioNuevoEquipoConUsuarioInexistenteDevuelveNoAutorizado() throws Exception {
        mockMvc.perform(get("/equipos/nuevo").session(sesion(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void crearEquipoGuardaYRedirigeAlListadoConConfirmacion(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(post("/equipos/nuevo").session(session).param("nombre", "Proyecto web"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Equipo creado correctamente"));

        assertThat(equipoService.findAllOrdenadoPorNombre())
                .extracting(EquipoData::getNombre).containsExactly("Proyecto web");
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proyecto web")))
                .andExpect(content().string(containsString("Equipo creado correctamente")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " \t\n"})
    public void crearEquipoSinNombreMuestraErrorYNoGuarda(String nombre) throws Exception {
        UsuarioData usuario = crearUsuario(false);
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                post("/equipos/nuevo").session(sesion(usuario.getId()));
        if (nombre != null) {
            peticion.param("nombre", nombre);
        }

        mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(view().name("formNuevoEquipo"))
                .andExpect(model().attribute("usuario", usuario))
                .andExpect(model().attribute("equipoData", hasProperty("nombre", equalTo(nombre))))
                .andExpect(model().attribute("error", "El nombre del equipo no puede estar vacío"))
                .andExpect(content().string(containsString("El nombre del equipo no puede estar vacío")))
                .andExpect(content().string(containsString("Cerrar sesión Usuario de equipos")))
                .andExpect(flash().attributeCount(0));

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void crearEquipoSinSesionDevuelveNoAutorizadoYNoGuarda() throws Exception {
        mockMvc.perform(post("/equipos/nuevo").param("nombre", "Equipo sin sesión"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void crearEquipoConUsuarioInexistenteDevuelveNoAutorizadoYNoGuarda() throws Exception {
        mockMvc.perform(post("/equipos/nuevo").session(sesion(Long.MAX_VALUE)).param("nombre", "Equipo ajeno"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void unirseAñadeSoloAlUsuarioDeSesionYMuestraConfirmacion(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        UsuarioData datosOtro = new UsuarioData();
        datosOtro.setEmail("otro@ua.es");
        datosOtro.setPassword("123");
        UsuarioData otro = usuarioService.registrar(datosOtro);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/unirse\"")))
                .andExpect(content().string(containsString(">Unirme</button>")));
        mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse").session(session)
                        .param("idUsuario", otro.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Te has unido al equipo correctamente"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);
        assertThat(equipoService.equiposUsuario(otro.getId())).isEmpty();
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Te has unido al equipo correctamente")))
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/salir\"")))
                .andExpect(content().string(not(containsString("action=\"/equipos/" + equipo.getId() + "/unirse\""))));
    }

    @Test
    public void unirseSiYaEsMiembroMuestraErrorYConservaPertenencia() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirse").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("error", "El usuario ya pertenece al equipo"))
                .andExpect(flash().attributeCount(1));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El usuario ya pertenece al equipo")));
    }

    @Test
    public void unirseAEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/unirse").session(sesion(usuario.getId())))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));

        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void unirseSinUsuarioValidoDevuelveNoAutorizado(boolean sesionInexistente) throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Participación");
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                post("/equipos/" + equipo.getId() + "/unirse");
        if (sesionInexistente) {
            peticion.session(sesion(Long.MAX_VALUE));
        }
        mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
    }

    @Test
    public void unirseCompruebaSesionAntesDeBuscarEquipo() throws Exception {
        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/unirse"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void unirseMedianteGetNoModificaPertenencia() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Participación");

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/unirse").session(sesion(usuario.getId())))
                .andExpect(status().isMethodNotAllowed());

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void salirEliminaSoloLaPertenenciaDelUsuarioDeSesion(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        UsuarioData datosOtro = new UsuarioData();
        datosOtro.setEmail("otro@ua.es");
        datosOtro.setPassword("123");
        UsuarioData otro = usuarioService.registrar(datosOtro);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        EquipoData otroEquipo = equipoService.crearEquipo("Otro equipo");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), otro.getId());
        equipoService.añadirUsuarioAEquipo(otroEquipo.getId(), usuario.getId());
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/salir\"")))
                .andExpect(content().string(containsString(">Salir</button>")));
        mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir").session(session)
                        .param("idUsuario", otro.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Has salido del equipo correctamente"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(otro);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(otroEquipo);
        assertThat(equipoService.equiposUsuario(otro.getId())).containsExactly(equipo);
        assertThat(equipoService.usuariosEquipo(otroEquipo.getId())).containsExactly(usuario);
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Has salido del equipo correctamente")))
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/unirse\"")))
                .andExpect(content().string(not(containsString("action=\"/equipos/" + equipo.getId() + "/salir\""))));
    }

    @Test
    public void salirSinPertenecerAlEquipoMuestraError() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("error", "El usuario no pertenece al equipo"))
                .andExpect(flash().attributeCount(1));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El usuario no pertenece al equipo")));
    }

    @Test
    public void salirDeEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/salir").session(sesion(usuario.getId())))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void salirSinUsuarioValidoDevuelveNoAutorizado(boolean sesionInexistente) throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                post("/equipos/" + equipo.getId() + "/salir");
        if (sesionInexistente) {
            peticion.session(sesion(Long.MAX_VALUE));
        }
        mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
    }

    @Test
    public void salirCompruebaSesionAntesDeBuscarEquipo() throws Exception {
        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/salir"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void salirMedianteGetNoModificaPertenencia() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Participación");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/salir").session(sesion(usuario.getId())))
                .andExpect(status().isMethodNotAllowed());

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void formularioEditarEquipoMuestraNombreActualYEnlaces(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        EquipoData equipo = equipoService.crearEquipo("Proyecto actual");
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/editar").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("formEditarEquipo"))
                .andExpect(model().attribute("usuario", usuario))
                .andExpect(model().attribute("equipoData", equipo))
                .andExpect(content().string(containsString("method=\"post\" action=\"/equipos/" + equipo.getId() + "/editar\"")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("value=\"Proyecto actual\"")))
                .andExpect(content().string(containsString(">Guardar cambios</button>")))
                .andExpect(content().string(containsString("href=\"/equipos\">Cancelar</a>")))
                .andExpect(content().string(containsString("Cerrar sesión Usuario de equipos")));

        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/equipos/" + equipo.getId() + "/editar\">Editar</a>")));
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto actual");
    }

    @Test
    public void formularioEditarEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(get("/equipos/" + Long.MAX_VALUE + "/editar").session(sesion(usuario.getId())))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void formularioEditarEquipoSinUsuarioValidoDevuelveNoAutorizado(boolean sesionInexistente) throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto actual");
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                get("/equipos/" + equipo.getId() + "/editar");
        if (sesionInexistente) {
            peticion.session(sesion(Long.MAX_VALUE));
        }

        mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @Test
    public void formularioEditarCompruebaSesionAntesDeBuscarEquipo() throws Exception {
        mockMvc.perform(get("/equipos/" + Long.MAX_VALUE + "/editar"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void guardarEquipoModificaElDeLaRutaYConservaMiembros(boolean administrador) throws Exception {
        UsuarioData usuario = crearUsuario(administrador);
        EquipoData equipo = equipoService.crearEquipo("Proyecto original");
        EquipoData otro = equipoService.crearEquipo("Otro equipo");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        MockHttpSession session = sesion(usuario.getId());

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/editar").session(session)
                        .param("nombre", "Proyecto renovado").param("id", otro.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Equipo modificado correctamente"));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto renovado");
        assertThat(equipoService.recuperarEquipo(otro.getId()).getNombre()).isEqualTo("Otro equipo");
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId()))
                .extracting(EquipoData::getNombre).containsExactly("Proyecto renovado");
        mockMvc.perform(get("/equipos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Equipo modificado correctamente")))
                .andExpect(content().string(containsString("Proyecto renovado")))
                .andExpect(content().string(not(containsString("Proyecto original"))));
        mockMvc.perform(get("/equipos/" + equipo.getId() + "/miembros").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proyecto renovado")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " \t\n"})
    public void guardarEquipoSinNombreMuestraErrorYConservaNombreOriginal(String nombre) throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Proyecto original");
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                post("/equipos/" + equipo.getId() + "/editar").session(sesion(usuario.getId()));
        if (nombre != null) {
            peticion.param("nombre", nombre);
        }

        mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(view().name("formEditarEquipo"))
                .andExpect(model().attribute("usuario", usuario))
                .andExpect(model().attribute("equipoData", hasProperty("id", equalTo(equipo.getId()))))
                .andExpect(model().attribute("equipoData", hasProperty("nombre", equalTo(nombre))))
                .andExpect(model().attribute("error", "El nombre del equipo no puede estar vacío"))
                .andExpect(content().string(containsString("El nombre del equipo no puede estar vacío")))
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/editar\"")))
                .andExpect(flash().attributeCount(0));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto original");
    }

    @Test
    public void guardarEquipoConNombreDuplicadoMuestraErrorYConservaFormulario() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Proyecto original");
        EquipoData otro = equipoService.crearEquipo("Nombre ocupado");

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/editar").session(sesion(usuario.getId()))
                        .param("nombre", otro.getNombre()).param("id", otro.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("formEditarEquipo"))
                .andExpect(model().attribute("equipoData", hasProperty("id", equalTo(equipo.getId()))))
                .andExpect(model().attribute("error", "Ya existe otro equipo con ese nombre"))
                .andExpect(content().string(containsString("Ya existe otro equipo con ese nombre")))
                .andExpect(content().string(containsString("value=\"Nombre ocupado\"")))
                .andExpect(content().string(containsString("action=\"/equipos/" + equipo.getId() + "/editar\"")))
                .andExpect(flash().attributeCount(0));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto original");
        assertThat(equipoService.recuperarEquipo(otro.getId()).getNombre()).isEqualTo("Nombre ocupado");
    }

    @Test
    public void guardarEquipoConSuPropioNombreTieneExito() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Proyecto original");

        mockMvc.perform(post("/equipos/" + equipo.getId() + "/editar").session(sesion(usuario.getId()))
                        .param("nombre", equipo.getNombre()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Equipo modificado correctamente"));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo(equipo.getNombre());
    }

    @Test
    public void guardarEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/editar").session(sesion(usuario.getId()))
                        .param("nombre", "Proyecto renovado"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void guardarEquipoSinUsuarioValidoDevuelveNoAutorizado(boolean sesionInexistente) throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Proyecto original");
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion =
                post("/equipos/" + equipo.getId() + "/editar").param("nombre", "Proyecto renovado");
        if (sesionInexistente) {
            peticion.session(sesion(Long.MAX_VALUE));
        }

        mockMvc.perform(peticion)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto original");
    }

    @Test
    public void guardarEquipoCompruebaSesionAntesDeBuscarEquipo() throws Exception {
        mockMvc.perform(post("/equipos/" + Long.MAX_VALUE + "/editar").param("nombre", "Proyecto renovado"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
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
                        + "</td>\\s*<td>\\s*<a href=\"/equipos/" + alfa.getId()
                        + "/miembros\">Alfa</a>.*<td>" + zeta.getId()
                        + "</td>\\s*<td>\\s*<a href=\"/equipos/" + zeta.getId() + "/miembros\">Zeta</a>.*")))
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

    @Test
    public void miembrosMuestraSoloLosUsuariosDelEquipoSinContraseñas() throws Exception {
        UsuarioData visitante = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Desarrollo");
        UsuarioData miembro = new UsuarioData();
        miembro.setEmail("miembro@ua.es");
        miembro.setNombre("Ana del equipo");
        miembro.setPassword("clave-miembro-secreta");
        miembro = usuarioService.registrar(miembro);
        equipoService.añadirUsuarioAEquipo(equipo.getId(), miembro.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), visitante.getId());

        UsuarioData ajeno = new UsuarioData();
        ajeno.setEmail("ajeno@ua.es");
        ajeno.setPassword("clave-ajeno");
        ajeno = usuarioService.registrar(ajeno);
        EquipoData otroEquipo = equipoService.crearEquipo("Otro equipo");
        equipoService.añadirUsuarioAEquipo(otroEquipo.getId(), ajeno.getId());

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/miembros")
                        .session(sesion(visitante.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("miembrosEquipo"))
                .andExpect(model().attribute("equipo", equipo))
                .andExpect(model().attribute("usuario", visitante))
                .andExpect(model().attribute("miembros", containsInAnyOrder(miembro, visitante)))
                .andExpect(content().string(containsString("Desarrollo")))
                .andExpect(content().string(containsString("<td>" + miembro.getId() + "</td>")))
                .andExpect(content().string(containsString("<td>Ana del equipo</td>")))
                .andExpect(content().string(containsString("<td>miembro@ua.es</td>")))
                .andExpect(content().string(not(containsString("ajeno@ua.es"))))
                .andExpect(content().string(not(containsString("clave-miembro-secreta"))))
                .andExpect(content().string(not(containsString("clave-equipos"))))
                .andExpect(content().string(not(containsString("Este equipo no tiene miembros."))))
                .andExpect(content().string(containsString("Cerrar sesión Usuario de equipos")))
                .andExpect(content().string(containsString("href=\"/equipos\">Volver al listado de equipos</a>")));
    }

    @Test
    public void equipoSinMiembrosMuestraMensajeAUsuarioAjeno() throws Exception {
        UsuarioData usuario = crearUsuario(false);
        EquipoData equipo = equipoService.crearEquipo("Vacío");

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/miembros").session(sesion(usuario.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("miembrosEquipo"))
                .andExpect(model().attribute("miembros", empty()))
                .andExpect(content().string(containsString("Este equipo no tiene miembros.")));
    }

    @Test
    public void administradorPuedeConsultarMiembros() throws Exception {
        UsuarioData administrador = crearUsuario(true);
        EquipoData equipo = equipoService.crearEquipo("Administración");

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/miembros")
                        .session(sesion(administrador.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("miembrosEquipo"));
    }

    @Test
    public void miembrosDeEquipoInexistenteDevuelveNoEncontrado() throws Exception {
        UsuarioData usuario = crearUsuario(false);

        mockMvc.perform(get("/equipos/" + Long.MAX_VALUE + "/miembros").session(sesion(usuario.getId())))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Equipo no encontrado"));
    }

    @Test
    public void miembrosSinSesionDevuelveNoAutorizadoAntesDeBuscarEquipo() throws Exception {
        mockMvc.perform(get("/equipos/" + Long.MAX_VALUE + "/miembros"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }

    @Test
    public void miembrosConUsuarioInexistenteDevuelveNoAutorizado() throws Exception {
        EquipoData equipo = equipoService.crearEquipo("Privado");

        mockMvc.perform(get("/equipos/" + equipo.getId() + "/miembros")
                        .session(sesion(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Usuario no autorizado"));
    }
}
