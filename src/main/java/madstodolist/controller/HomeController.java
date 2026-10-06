package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/about")
    public String about(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario != null ? usuarioService.findById(idUsuario) : null;
        model.addAttribute("usuario", usuario);
        return "about";
    }

}
