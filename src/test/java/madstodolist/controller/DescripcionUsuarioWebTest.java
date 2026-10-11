package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    public void descripcionUsuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/registrados/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Usuario no encontrado"));
    }
}
