package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario != null ? usuarioService.findById(idUsuario) : null;
        if (usuario == null || !usuario.isAdministrador()) {
            throw new UsuarioNoAdministradorException();
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarios", usuarioService.findAll());
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable("id") Long idUsuario, Model model) {
        UsuarioData usuario = usuarioService.findById(idUsuario);
        if (usuario == null) {
            throw new UsuarioNotFoundException();
        }
        model.addAttribute("usuarioDescripcion", usuario);
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        UsuarioData usuarioLogeado = idUsuarioLogeado != null ? usuarioService.findById(idUsuarioLogeado) : null;
        model.addAttribute("usuario", usuarioLogeado);
        return "descripcionUsuario";
    }
}
