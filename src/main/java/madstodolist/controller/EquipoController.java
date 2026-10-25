package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private UsuarioData comprobarUsuarioLogeado() {
        Long idUsuario = managerUserSession.usuarioLogeado();
        UsuarioData usuario = idUsuario != null ? usuarioService.findById(idUsuario) : null;
        if (usuario == null) {
            throw new UsuarioNoLogeadoException();
        }
        return usuario;
    }

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        model.addAttribute("usuario", comprobarUsuarioLogeado());
        model.addAttribute("equipos", equipoService.findAllOrdenadoPorNombre());
        return "listaEquipos";
    }

    @GetMapping("/equipos/nuevo")
    public String formNuevoEquipo(Model model) {
        model.addAttribute("usuario", comprobarUsuarioLogeado());
        model.addAttribute("equipoData", new EquipoData());
        return "formNuevoEquipo";
    }

    @GetMapping("/equipos/{id}/miembros")
    public String miembrosEquipo(@PathVariable("id") Long idEquipo, Model model) {
        model.addAttribute("usuario", comprobarUsuarioLogeado());
        EquipoData equipo;
        try {
            equipo = equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException ex) {
            throw new EquipoNotFoundException();
        }
        model.addAttribute("equipo", equipo);
        model.addAttribute("miembros", equipoService.usuariosEquipo(idEquipo));
        return "miembrosEquipo";
    }
}
