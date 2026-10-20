package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.model.Equipo;
import madstodolist.repository.EquipoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {

    @Autowired
    EquipoRepository equipoRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        Equipo equipo = new Equipo(nombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) return null;
        return modelMapper.map(equipo, EquipoData.class);
    }
}
