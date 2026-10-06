package madstodolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AcercaDeWebTest {

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/registro\">Registro</a>")));
    }
}
