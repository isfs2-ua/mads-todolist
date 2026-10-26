package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import madstodolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    @Autowired
    EquipoRepository equipoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EquipoServiceException("El nombre del equipo no puede estar vacío");
        }
        Equipo equipo = new Equipo(nombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public EquipoData modificarEquipo(Long idEquipo, String nombre) {
        if (idEquipo == null) {
            throw new EquipoServiceException("El identificador del equipo es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EquipoServiceException("El nombre del equipo no puede estar vacío");
        }
        Equipo equipo = equipoRepository.findById(idEquipo)
                .orElseThrow(() -> new EquipoServiceException("No existe equipo con id " + idEquipo));
        if (equipoRepository.existsByNombreAndIdNot(nombre, idEquipo)) {
            throw new EquipoServiceException("Ya existe otro equipo con ese nombre");
        }
        equipo.setNombre(nombre);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public void eliminarEquipo(Long idEquipo) {
        if (idEquipo == null) {
            throw new EquipoServiceException("El identificador del equipo es obligatorio");
        }
        Equipo equipo = equipoRepository.findById(idEquipo)
                .orElseThrow(() -> new EquipoServiceException("No existe equipo con id " + idEquipo));
        equipoRepository.delete(equipo);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("No existe equipo con id " + id);
        }
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre() {
        return equipoRepository.findAll().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .sorted(Comparator.comparing(EquipoData::getNombre))
                .collect(Collectors.toList());
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long id, Long id1) {
        if (id == null || id1 == null) {
            throw new EquipoServiceException("Los identificadores del equipo y del usuario son obligatorios");
        }
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        Usuario usuario = usuarioRepository.findById(id1).orElse(null);
        if (equipo == null || usuario == null) {
            throw new EquipoServiceException("No existe equipo o usuario");
        }
        if (equipo.getUsuarios().contains(usuario)) {
            throw new EquipoServiceException("El usuario ya pertenece al equipo");
        }
        equipo.addUsuario(usuario);
    }

    @Transactional
    public void eliminarUsuarioDeEquipo(Long idEquipo, Long idUsuario) {
        if (idEquipo == null || idUsuario == null) {
            throw new EquipoServiceException("Los identificadores del equipo y del usuario son obligatorios");
        }
        Equipo equipo = equipoRepository.findById(idEquipo)
                .orElseThrow(() -> new EquipoServiceException("No existe equipo con id " + idEquipo));
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EquipoServiceException("No existe usuario con id " + idUsuario));
        if (!equipo.getUsuarios().contains(usuario)) {
            throw new EquipoServiceException("El usuario no pertenece al equipo");
        }
        equipo.getUsuarios().remove(usuario);
        usuario.getEquipos().remove(equipo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioData> usuariosEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("No existe equipo con id " + id);
        }
        // Hacemos uso de Java Stream API para mapear la lista de entidades a DTOs.
        return equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipoData> equiposUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("No existe usuario con id " + idUsuario);
        }
        return usuario.getEquipos().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .sorted(Comparator.comparing(EquipoData::getNombre))
                .collect(Collectors.toList());
    }
}
