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
