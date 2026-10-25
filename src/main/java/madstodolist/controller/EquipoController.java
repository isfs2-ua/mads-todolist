package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario != null ? usuarioService.findById(idUsuario) : null;
        if (usuario == null) {
            throw new UsuarioNoLogeadoException();
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("equipos", equipoService.findAllOrdenadoPorNombre());
        return "listaEquipos";
    }
}
