package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.ParticipanteData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
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
    private EquipoRepository equipoRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<EquipoData> findAll() {
        return equipoRepository.findAllByOrderByNombreAsc().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EquipoData findById(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        return equipo == null ? null : modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public List<ParticipanteData> allUsuariosEquipo(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new EquipoServiceException("No existe equipo con id " + equipoId));
        return equipo.getUsuarios().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(usuario -> modelMapper.map(usuario, ParticipanteData.class))
                .collect(Collectors.toList());
    }
}
