package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(content().string(containsString("<th scope=\"col\">Identificador</th>")))
                .andExpect(content().string(containsString("<th scope=\"col\">Correo electrónico</th>")))
                .andExpect(content().string(matchesPattern("(?s).*<tbody>\\s*"
                        + "(<tr>\\s*<td>[^<]+</td>\\s*<td>[^<]+</td>\\s*</tr>\\s*){2}"
                        + "</tbody>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<tr>\\s*<td>" + primero.getId()
                        + "</td>\\s*<td>primero@ua[.]es</td>\\s*</tr>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<tr>\\s*<td>" + segundo.getId()
                        + "</td>\\s*<td>segundo@ua[.]es</td>\\s*</tr>.*")))
                .andExpect(content().string(not(containsString("clave-primero"))))
                .andExpect(content().string(not(containsString("clave-segundo"))))
                .andExpect(content().string(not(containsString("No hay usuarios registrados."))));
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
