package madstodolist.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "No autorizado: se necesita permiso de administrador")
public class UsuarioNoAdministradorException extends RuntimeException {
}
