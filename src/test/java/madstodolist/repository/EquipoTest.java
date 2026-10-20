package madstodolist.repository;

import madstodolist.model.Equipo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoTest {

    @Autowired
    private EquipoRepository equipoRepository;

    @Test
    public void crearEquipo() {
        Equipo equipo = new Equipo("Proyecto P1");
        assertThat(equipo.getNombre()).isEqualTo("Proyecto P1");
    }

    @Test
    @Transactional
    public void grabarYBuscarEquipo() {
        // GIVEN
        // Un equipo nuevo
        Equipo equipo = new Equipo("Proyecto P1");

        // Probamos el constructor vacío, necesario para que funcione JPA/Hibernate
        equipo = new Equipo();

        // Creamos ya el equipo nuevo
        equipo = new Equipo("Proyecto P1");

        // WHEN
        // Salvamos el equipo en la base de datos
        equipoRepository.save(equipo);

        // THEN
        // Su identificador se ha actualizado y lo podemos
        // usar para recuperarlo de la base de datos
        Long equipoId = equipo.getId();
        assertThat(equipoId).isNotNull();
        Equipo equipoDB = equipoRepository.findById(equipoId).orElse(null);
        assertThat(equipoDB).isNotNull();
        assertThat(equipoDB.getNombre()).isEqualTo("Proyecto P1");
    }
}
