package madstodolist.repository;

import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
@Transactional
public class EquipoTest {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void guardarEquipoSinParticipantes() {
        Equipo equipo = equipoRepository.save(new Equipo("Desarrollo"));
        entityManager.flush();
        entityManager.clear();

        Equipo recuperado = equipoRepository.findById(equipo.getId()).get();
        assertThat(recuperado.getNombre()).isEqualTo("Desarrollo");
        assertThat(recuperado.getUsuarios()).isEmpty();
    }

    @Test
    public void usuarioPerteneceAVariosEquiposYEquipoTieneVariosUsuarios() {
        Usuario ana = usuarioRepository.save(new Usuario("ana@ua.es"));
        Usuario juan = usuarioRepository.save(new Usuario("juan@ua.es"));
        Equipo desarrollo = new Equipo("Desarrollo");
        Equipo soporte = new Equipo("Soporte");

        desarrollo.addUsuario(ana);
        juan.addEquipo(desarrollo);
        ana.addEquipo(soporte);
        desarrollo.addUsuario(ana);

        assertThat(ana.getEquipos()).containsExactlyInAnyOrder(desarrollo, soporte);
        assertThat(desarrollo.getUsuarios()).containsExactlyInAnyOrder(ana, juan);

        equipoRepository.save(desarrollo);
        equipoRepository.save(soporte);
        entityManager.flush();
        entityManager.clear();

        assertThat(equipoRepository.findById(desarrollo.getId()).get().getUsuarios())
                .extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("ana@ua.es", "juan@ua.es");
        assertThat(equipoRepository.findById(soporte.getId()).get().getUsuarios())
                .extracting(Usuario::getEmail).containsExactly("ana@ua.es");
        assertThat(usuarioRepository.findById(ana.getId()).get().getEquipos())
                .extracting(Equipo::getNombre)
                .containsExactlyInAnyOrder("Desarrollo", "Soporte");
        assertThat(usuarioRepository.findById(juan.getId()).get().getEquipos())
                .extracting(Equipo::getNombre).containsExactly("Desarrollo");
    }

    @Test
    public void listadoOrdenadoAlfabeticamente() {
        equipoRepository.save(new Equipo("Soporte"));
        equipoRepository.save(new Equipo("Administración"));
        equipoRepository.save(new Equipo("Desarrollo"));
        entityManager.flush();
        entityManager.clear();

        assertThat(equipoRepository.findAllByOrderByNombreAsc())
                .extracting(Equipo::getNombre)
                .containsExactly("Administración", "Desarrollo", "Soporte");
    }

    @Test
    public void listadoSinEquiposDevuelveListaVacia() {
        assertThat(equipoRepository.findAllByOrderByNombreAsc()).isEmpty();
    }
}
