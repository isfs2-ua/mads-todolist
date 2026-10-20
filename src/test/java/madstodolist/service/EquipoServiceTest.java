package madstodolist.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import madstodolist.dto.EquipoData;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    EquipoService equipoService;

    @Test
    public void crearRecuperarEquipo() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }
}