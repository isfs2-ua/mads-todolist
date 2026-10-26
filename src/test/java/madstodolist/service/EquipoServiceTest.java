package madstodolist.service;

import madstodolist.dto.UsuarioData;
import madstodolist.model.Usuario;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import madstodolist.dto.EquipoData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Test
    public void crearRecuperarEquipo() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " \t\n"})
    public void crearEquipoSinNombreLanzaExcepcion(String nombre) {
        assertThatThrownBy(() -> equipoService.crearEquipo(nombre))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El nombre del equipo no puede estar vacío");

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void modificarNombreEquipo() {
        // GIVEN: un equipo existente
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");

        // WHEN: modificamos su nombre
        EquipoData equipoModificado = equipoService.modificarEquipo(equipo.getId(), "Proyecto renovado");

        // THEN: conserva su identificador y el nuevo nombre queda guardado
        assertThat(equipoModificado.getId()).isEqualTo(equipo.getId());
        assertThat(equipoModificado.getNombre()).isEqualTo("Proyecto renovado");
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto renovado");
        assertThat(equipoService.findAllOrdenadoPorNombre()).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " \t\n"})
    public void modificarEquipoSinNombreLanzaExcepcion(String nombre) {
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");

        assertThatThrownBy(() -> equipoService.modificarEquipo(equipo.getId(), nombre))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El nombre del equipo no puede estar vacío");

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto inicial");
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(equipo);
    }

    @Test
    public void modificarEquipoConNombreDeOtroEquipoLanzaExcepcion() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");
        EquipoData otroEquipo = equipoService.crearEquipo("Proyecto existente");

        assertThatThrownBy(() -> equipoService.modificarEquipo(equipo.getId(), otroEquipo.getNombre()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("Ya existe otro equipo con ese nombre");

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto inicial");
        assertThat(equipoService.recuperarEquipo(otroEquipo.getId()).getNombre())
                .isEqualTo("Proyecto existente");
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactlyInAnyOrder(equipo, otroEquipo);
    }

    @Test
    public void modificarEquipoConSuPropioNombre() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");
        EquipoData otroEquipo = equipoService.crearEquipo("Otro proyecto");

        EquipoData equipoModificado = equipoService.modificarEquipo(equipo.getId(), equipo.getNombre());

        assertThat(equipoModificado.getId()).isEqualTo(equipo.getId());
        assertThat(equipoModificado.getNombre()).isEqualTo("Proyecto inicial");
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto inicial");
        assertThat(equipoService.recuperarEquipo(otroEquipo.getId()).getNombre())
                .isEqualTo("Otro proyecto");
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactlyInAnyOrder(equipo, otroEquipo);
    }

    @Test
    public void modificarNombreConservaParticipantesYActualizaEquiposUsuario() {
        // GIVEN: dos miembros de un equipo y otra pertenencia del primer usuario
        UsuarioData datosPrimero = new UsuarioData();
        datosPrimero.setEmail("primero@ua");
        datosPrimero.setPassword("123");
        UsuarioData primero = usuarioService.registrar(datosPrimero);
        UsuarioData datosSegundo = new UsuarioData();
        datosSegundo.setEmail("segundo@ua");
        datosSegundo.setPassword("456");
        UsuarioData segundo = usuarioService.registrar(datosSegundo);
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");
        EquipoData otroEquipo = equipoService.crearEquipo("Otro proyecto");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), primero.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), segundo.getId());
        equipoService.añadirUsuarioAEquipo(otroEquipo.getId(), primero.getId());

        // WHEN: renombramos el equipo
        equipoService.modificarEquipo(equipo.getId(), "Equipo renovado");

        // THEN: se conservan los miembros y el nuevo nombre aparece en todas las consultas
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactlyInAnyOrder(primero, segundo);
        assertThat(equipoService.equiposUsuario(primero.getId())).extracting("id", "nombre")
                .containsExactly(tuple(equipo.getId(), "Equipo renovado"),
                        tuple(otroEquipo.getId(), "Otro proyecto"));
        assertThat(equipoService.equiposUsuario(segundo.getId())).extracting("id", "nombre")
                .containsExactly(tuple(equipo.getId(), "Equipo renovado"));
        assertThat(equipoService.findAllOrdenadoPorNombre()).extracting("id", "nombre")
                .containsExactly(tuple(equipo.getId(), "Equipo renovado"),
                        tuple(otroEquipo.getId(), "Otro proyecto"));
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Equipo renovado");
        assertThat(equipoService.usuariosEquipo(otroEquipo.getId())).containsExactly(primero);
    }

    @Test
    public void modificarEquipoInexistenteLanzaExcepcion() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");
        Long idEquipoInexistente = Long.MAX_VALUE;

        assertThatThrownBy(() -> equipoService.modificarEquipo(idEquipoInexistente, "Proyecto renovado"))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id " + idEquipoInexistente);

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto inicial");
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(equipo);
    }

    @Test
    public void modificarEquipoConIdentificadorNuloLanzaExcepcion() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto inicial");

        assertThatThrownBy(() -> equipoService.modificarEquipo(null, "Proyecto renovado"))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El identificador del equipo es obligatorio");

        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto inicial");
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(equipo);
    }

    @Test
    public void eliminarEquipoVacio() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto a eliminar");
        EquipoData otroEquipo = equipoService.crearEquipo("Otro proyecto");

        equipoService.eliminarEquipo(equipo.getId());

        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(otroEquipo);
        assertThatThrownBy(() -> equipoService.recuperarEquipo(equipo.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id " + equipo.getId());
        assertThat(equipoService.recuperarEquipo(otroEquipo.getId()).getNombre())
                .isEqualTo("Otro proyecto");
    }

    @Test
    public void eliminarEquipoConMiembrosConservaUsuariosYOtrasPertenencias() {
        // GIVEN: dos miembros del equipo y uno de ellos también pertenece a otro equipo
        UsuarioData datosPrimero = new UsuarioData();
        datosPrimero.setEmail("primero@ua");
        datosPrimero.setPassword("123");
        UsuarioData primero = usuarioService.registrar(datosPrimero);
        UsuarioData datosSegundo = new UsuarioData();
        datosSegundo.setEmail("segundo@ua");
        datosSegundo.setPassword("456");
        UsuarioData segundo = usuarioService.registrar(datosSegundo);
        EquipoData equipo = equipoService.crearEquipo("Proyecto a eliminar");
        EquipoData otroEquipo = equipoService.crearEquipo("Otro proyecto");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), primero.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), segundo.getId());
        equipoService.añadirUsuarioAEquipo(otroEquipo.getId(), segundo.getId());

        // WHEN: eliminamos el equipo con sus miembros
        equipoService.eliminarEquipo(equipo.getId());

        // THEN: desaparecen el equipo y sus pertenencias, conservando las cuentas y el otro equipo
        assertThatThrownBy(() -> equipoService.recuperarEquipo(equipo.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id " + equipo.getId());
        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(otroEquipo);
        assertThat(usuarioService.findById(primero.getId())).isEqualTo(primero);
        assertThat(usuarioService.findById(segundo.getId())).isEqualTo(segundo);
        assertThat(equipoService.equiposUsuario(primero.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(segundo.getId())).containsExactly(otroEquipo);
        assertThat(equipoService.usuariosEquipo(otroEquipo.getId())).containsExactly(segundo);
        assertThat(equipoService.recuperarEquipo(otroEquipo.getId()).getNombre())
                .isEqualTo("Otro proyecto");
    }

    @Test
    public void eliminarEquipoInexistenteLanzaExcepcion() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto existente");
        Long idEquipoInexistente = Long.MAX_VALUE;

        assertThatThrownBy(() -> equipoService.eliminarEquipo(idEquipoInexistente))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id " + idEquipoInexistente);

        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(equipo);
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto existente");
    }

    @Test
    public void eliminarEquipoConIdentificadorNuloLanzaExcepcion() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto existente");

        assertThatThrownBy(() -> equipoService.eliminarEquipo(null))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El identificador del equipo es obligatorio");

        assertThat(equipoService.findAllOrdenadoPorNombre()).containsExactly(equipo);
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre())
                .isEqualTo("Proyecto existente");
    }

    @Test
    public void listadoEquiposOrdenAlfabetico() {
        // GIVEN
        // Dos equipos en la base de datos
        equipoService.crearEquipo("Proyecto BBB");
        equipoService.crearEquipo("Proyecto AAA");

        // WHEN
        // Recuperamos los equipos
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        // THEN
        // Los equipos están ordenados por nombre
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto AAA");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto BBB");
    }

    @Test
    public void añadirUsuarioAEquipo() {
        // GIVEN
        // Un usuario y un equipo en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        // Añadimos el usuario al equipo
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // THEN
        // El usuario pertenece al equipo
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@ua");
    }

    @Test
    public void añadirUsuarioQueYaPerteneceAlEquipoLanzaExcepcion() {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("miembro@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El usuario ya pertenece al equipo");

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void añadirConIdentificadorNuloLanzaExcepcion(boolean equipoNulo) {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("nuevo@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        Long idEquipo = equipoNulo ? null : equipo.getId();
        Long idUsuario = equipoNulo ? usuario.getId() : null;

        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(idEquipo, idUsuario))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("Los identificadores del equipo y del usuario son obligatorios");

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
    }

    @Test
    public void eliminarUsuarioDeEquipo() {
        // GIVEN: un usuario que pertenece a un equipo
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("miembro@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);

        // WHEN: el usuario deja el equipo
        equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario.getId());

        // THEN: desaparece la pertenencia, pero se conservan el usuario y el equipo
        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
        assertThat(usuarioService.findById(usuario.getId())).isEqualTo(usuario);
        assertThat(equipoService.recuperarEquipo(equipo.getId())).isEqualTo(equipo);
    }

    @Test
    public void eliminarPertenenciaConservaOtrosMiembrosYEquipos() {
        // GIVEN: dos usuarios que pertenecen a dos equipos
        UsuarioData datosPrimero = new UsuarioData();
        datosPrimero.setEmail("primero@ua");
        datosPrimero.setPassword("123");
        UsuarioData primero = usuarioService.registrar(datosPrimero);
        UsuarioData datosSegundo = new UsuarioData();
        datosSegundo.setEmail("segundo@ua");
        datosSegundo.setPassword("456");
        UsuarioData segundo = usuarioService.registrar(datosSegundo);
        EquipoData equipo1 = equipoService.crearEquipo("Proyecto 1");
        EquipoData equipo2 = equipoService.crearEquipo("Proyecto 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), primero.getId());
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), segundo.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), primero.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), segundo.getId());

        // WHEN: el primer usuario deja únicamente el primer equipo
        equipoService.eliminarUsuarioDeEquipo(equipo1.getId(), primero.getId());

        // THEN: desaparece solo esa pertenencia, en ambos sentidos
        assertThat(equipoService.usuariosEquipo(equipo1.getId())).containsExactly(segundo);
        assertThat(equipoService.equiposUsuario(primero.getId())).containsExactly(equipo2);
        assertThat(equipoService.usuariosEquipo(equipo2.getId())).containsExactlyInAnyOrder(primero, segundo);
        assertThat(equipoService.equiposUsuario(segundo.getId())).containsExactlyInAnyOrder(equipo1, equipo2);
    }

    @Test
    public void eliminarUsuarioQueNoPerteneceAlEquipoLanzaExcepcion() {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("ajeno@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El usuario no pertenece al equipo");

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void eliminarConIdentificadorNuloLanzaExcepcion(boolean equipoNulo) {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("miembro@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        Long idEquipo = equipoNulo ? null : equipo.getId();
        Long idUsuario = equipoNulo ? usuario.getId() : null;

        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(idEquipo, idUsuario))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("Los identificadores del equipo y del usuario son obligatorios");

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);
    }

    @Test
    public void eliminarDeEquipoInexistenteLanzaExcepcion() {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("miembro@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        Long idEquipoInexistente = Long.MAX_VALUE;

        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(idEquipoInexistente, usuario.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe equipo con id " + idEquipoInexistente);

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);
    }

    @Test
    public void eliminarUsuarioInexistenteLanzaExcepcion() {
        UsuarioData datosUsuario = new UsuarioData();
        datosUsuario.setEmail("miembro@ua");
        datosUsuario.setPassword("123");
        UsuarioData usuario = usuarioService.registrar(datosUsuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        Long idUsuarioInexistente = Long.MAX_VALUE;

        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(equipo.getId(), idUsuarioInexistente))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("No existe usuario con id " + idUsuarioInexistente);

        assertThat(equipoService.usuariosEquipo(equipo.getId())).containsExactly(usuario);
        assertThat(equipoService.equiposUsuario(usuario.getId())).containsExactly(equipo);
    }

    @Test
    public void recuperarEquiposDeUsuario() {
        // GIVEN
        // Un usuario y dos equipos en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo1 = equipoService.crearEquipo("Proyecto 1");
        EquipoData equipo2 = equipoService.crearEquipo("Proyecto 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), usuario.getId());

        // WHEN
        // Recuperamos los equipos del usuario
        List<EquipoData> equipos = equipoService.equiposUsuario(usuario.getId());

        // THEN
        // El usuario pertenece a los dos equipos
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto 1");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void comprobarExcepciones() {
        // Comprobamos las excepciones lanzadas por los métodos
        // recuperarEquipo, añadirUsuarioAEquipo, usuariosEquipo y equiposUsuario
        assertThatThrownBy(() -> equipoService.recuperarEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(1L, 1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.usuariosEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.equiposUsuario(1L))
                .isInstanceOf(EquipoServiceException.class);

        // Creamos un equipo pero no un usuario y comprobamos que también se lanza una excepción
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class);
    }
}
