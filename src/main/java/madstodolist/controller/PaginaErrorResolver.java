package madstodolist.controller;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaginaErrorResolver implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status,
                                        Map<String, Object> model) {
        Map<String, Object> datos = new HashMap<>(model);
        Object motivo = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String mensaje = "No se ha podido completar la solicitud. Inténtalo de nuevo más tarde.";
        if (status.is4xxClientError()) {
            mensaje = status == HttpStatus.NOT_FOUND ? "La página o el recurso solicitado no existe."
                    : "No se ha podido acceder al recurso solicitado.";
            if (motivo instanceof String && !((String) motivo).trim().isEmpty()) {
                mensaje = (String) motivo;
            }
        }
        datos.put("mensaje", mensaje);
        return new ModelAndView("error", datos, status);
    }
}
