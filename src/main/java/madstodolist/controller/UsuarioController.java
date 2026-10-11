package madstodolist.controller;

import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.findAll());
        return "listaUsuarios";
    }
}
