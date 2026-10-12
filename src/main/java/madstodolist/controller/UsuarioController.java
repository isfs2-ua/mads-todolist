package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private UsuarioData comprobarAdministrador() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario != null ? usuarioService.findById(idUsuario) : null;
        if (usuario == null || !usuario.isAdministrador()) {
            throw new UsuarioNoAdministradorException();
        }
        return usuario;
    }

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        UsuarioData usuario = comprobarAdministrador();
        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarios", usuarioService.findAll());
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable("id") Long idUsuario, Model model) {
        UsuarioData usuarioLogeado = comprobarAdministrador();
        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }
        model.addAttribute("usuarioDescripcion", usuario);
        model.addAttribute("usuario", usuarioLogeado);
        return "descripcionUsuario";
    }

    @PostMapping("/registrados/{id}/bloqueo")
    public String cambiarBloqueo(@PathVariable("id") Long idUsuario,
                                 @RequestParam boolean bloqueado) {
        comprobarAdministrador();
        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }
        if (bloqueado && usuario.isAdministrador()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede bloquear al administrador");
        }
        usuarioService.cambiarBloqueo(idUsuario, bloqueado);
        return "redirect:/registrados";
    }
}
