package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.ParticipanteData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import madstodolist.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    public void listadoDevuelveIdentificadoresYNombresOrdenados() {
        Equipo soporte = equipoRepository.save(new Equipo("Soporte"));
        Equipo administracion = equipoRepository.save(new Equipo("Administración"));
        Equipo desarrollo = equipoRepository.save(new Equipo("Desarrollo"));

        assertThat(equipoService.findAll())
                .extracting(EquipoData::getId, EquipoData::getNombre)
                .containsExactly(tuple(administracion.getId(), "Administración"),
                        tuple(desarrollo.getId(), "Desarrollo"), tuple(soporte.getId(), "Soporte"));
    }

    @Test
    public void listadoSinEquiposDevuelveListaVacia() {
        assertThat(equipoService.findAll()).isEmpty();
    }

    @Test
    public void consultaEquipoPorIdentificador() {
        Equipo equipo = equipoRepository.save(new Equipo("Desarrollo"));

        EquipoData resultado = equipoService.findById(equipo.getId());

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(equipo.getId());
        assertThat(resultado.getNombre()).isEqualTo("Desarrollo");
    }

    @Test
    public void consultaEquipoInexistenteDevuelveNull() {
        assertThat(equipoService.findById(-1L)).isNull();
    }

    @Test
    public void participantesIncluyenSoloUsuariosDelEquipoYPermitenPertenenciaMultiple() {
        Usuario ana = new Usuario("ana@ua.es");
        ana.setNombre("Ana");
        ana.setPassword("clave-ana");
        usuarioRepository.save(ana);
        Usuario juan = new Usuario("juan@ua.es");
        juan.setPassword("clave-juan");
        usuarioRepository.save(juan);
        Usuario externo = usuarioRepository.save(new Usuario("externo@ua.es"));

        Equipo desarrollo = new Equipo("Desarrollo");
        desarrollo.addUsuario(juan);
        desarrollo.addUsuario(ana);
        equipoRepository.save(desarrollo);
        Equipo soporte = new Equipo("Soporte");
        soporte.addUsuario(ana);
        soporte.addUsuario(externo);
        equipoRepository.save(soporte);

        // Sin transacción en el test: los DTO se consultan tras cerrar la del servicio.
        List<ParticipanteData> participantes = equipoService.allUsuariosEquipo(desarrollo.getId());
        assertThat(participantes)
                .extracting(ParticipanteData::getId, ParticipanteData::getNombre, ParticipanteData::getEmail)
                .containsExactly(tuple(ana.getId(), "Ana", "ana@ua.es"),
                        tuple(juan.getId(), null, "juan@ua.es"));
        assertThat(equipoService.allUsuariosEquipo(soporte.getId()))
                .extracting(ParticipanteData::getId)
                .containsExactly(ana.getId(), externo.getId());
    }

    @Test
    public void equipoSinParticipantesDevuelveListaVacia() {
        Equipo equipo = equipoRepository.save(new Equipo("Desarrollo"));

        assertThat(equipoService.allUsuariosEquipo(equipo.getId())).isEmpty();
    }

    @Test
    public void participantesDeEquipoInexistenteLanzanExcepcion() {
        assertThatThrownBy(() -> equipoService.allUsuariosEquipo(-1L))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id -1");
    }
}
