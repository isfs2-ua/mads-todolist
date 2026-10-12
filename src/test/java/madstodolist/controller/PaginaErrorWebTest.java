package madstodolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaginaErrorWebTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaginaErrorResolver errorResolver;

    private ResponseEntity<String> pedirPagina(String ruta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
        return restTemplate.exchange(ruta, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    public void accesoSinPermisosMuestraPaginaPropiaConMotivoYEstado401() {
        for (String ruta : new String[]{"/registrados", "/registrados/1"}) {
            ResponseEntity<String> respuesta = pedirPagina(ruta);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(respuesta.getBody()).contains("No autorizado", ">401</p>",
                    "No autorizado: se necesita permiso de administrador",
                    "/css/bootstrap.min.css")
                    .containsPattern("href=\"/login(?:;jsessionid=[^\"]+)?\"")
                    .containsPattern("href=\"/about(?:;jsessionid=[^\"]+)?\"")
                    .doesNotContain("Whitelabel Error Page");
        }
    }

    @Test
    public void rutaInexistenteMuestraPaginaPropiaConEstado404() {
        ResponseEntity<String> respuesta = pedirPagina("/pagina-que-no-existe");
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).contains("Recurso no encontrado", ">404</p>",
                "La página o el recurso solicitado no existe.")
                .doesNotContain("Whitelabel Error Page");
    }

    @Test
    public void falloInternoUsaMensajeGenericoSinMostrarDetalles() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Detalles internos de la base de datos");
        ModelAndView vista = errorResolver.resolveErrorView(request, HttpStatus.INTERNAL_SERVER_ERROR,
                Collections.singletonMap("status", 500));
        assertThat(vista.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(vista.getViewName()).isEqualTo("error");
        assertThat(vista.getModel().get("mensaje"))
                .isEqualTo("No se ha podido completar la solicitud. Inténtalo de nuevo más tarde.");
    }
}
