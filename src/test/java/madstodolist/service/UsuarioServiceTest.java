package madstodolist.service;

import madstodolist.dto.UsuarioData;
import org.junit.jupiter.api.Assertions;
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
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve el identificador del usuario de la BD
    Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setNombre("Usuario Ejemplo");
        usuario.setPassword("123");
        UsuarioData nuevoUsuario = usuarioService.registrar(usuario);
        return nuevoUsuario.getId();
    }

    @Test
    public void bloqueoSePersisteYPermiteHabilitarDeNuevoSinAfectarAOtrosUsuarios() {
        Long usuarioId = addUsuarioBD();
        UsuarioData otro = new UsuarioData();
        otro.setEmail("otro@ua.es");
        otro.setPassword("456");
        otro = usuarioService.registrar(otro);
        assertThat(otro.isBloqueado()).isFalse();
        assertThat(usuarioService.findById(usuarioId).isBloqueado()).isFalse();

        assertThat(usuarioService.cambiarBloqueo(usuarioId, true).isBloqueado()).isTrue();
        assertThat(usuarioService.findById(usuarioId).isBloqueado()).isTrue();
        assertThat(usuarioService.findByEmail("user@ua").isBloqueado()).isTrue();
        assertThat(usuarioService.findAll()).extracting("id", "bloqueado")
                .containsExactlyInAnyOrder(tuple(usuarioId, true), tuple(otro.getId(), false));

        assertThat(usuarioService.cambiarBloqueo(usuarioId, false).isBloqueado()).isFalse();
        UsuarioData habilitado = usuarioService.findById(usuarioId);
        assertThat(habilitado.isBloqueado()).isFalse();
        assertThat(habilitado.getEmail()).isEqualTo("user@ua");
        assertThat(habilitado.getNombre()).isEqualTo("Usuario Ejemplo");
        assertThat(habilitado.getPassword()).isEqualTo("123");
        assertThat(usuarioService.findById(otro.getId()).isBloqueado()).isFalse();
    }

    @Test
    public void cambiarBloqueoDeUsuarioInexistenteDevuelveErrorSinCrearUsuarios() {
        for (boolean bloqueado : new boolean[]{true, false}) {
            assertThatThrownBy(() -> usuarioService.cambiarBloqueo(Long.MAX_VALUE, bloqueado))
                    .isInstanceOf(UsuarioServiceException.class)
                    .hasMessage("El usuario no existe");
        }
        assertThat(usuarioService.findAll()).isEmpty();
    }

    @Test
    public void loginRechazaUsuarioBloqueadoYPermiteAccesoTrasHabilitarlo() {
        Long usuarioId = addUsuarioBD();
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);

        usuarioService.cambiarBloqueo(usuarioId, true);
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.USER_BLOCKED);
        assertThat(usuarioService.login("user@ua", "incorrecta")).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);
        assertThat(usuarioService.login("inexistente@ua.es", "123")).isEqualTo(UsuarioService.LoginStatus.USER_NOT_FOUND);

        usuarioService.cambiarBloqueo(usuarioId, false);
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
    }

    @Test
    public void registroAdministradorPersisteRolYPermiteUsuariosNormales() {
        assertThat(usuarioService.existeAdministrador()).isFalse();
        Long usuarioId = addUsuarioBD();
        assertThat(usuarioService.findById(usuarioId).isAdministrador()).isFalse();
        assertThat(usuarioService.existeAdministrador()).isFalse();

        UsuarioData administrador = new UsuarioData();
        administrador.setEmail("admin@ua.es");
        administrador.setPassword("123");
        administrador.setAdministrador(true);
        UsuarioData registrado = usuarioService.registrar(administrador);

        assertThat(registrado.isAdministrador()).isTrue();
        assertThat(usuarioService.findById(registrado.getId()).isAdministrador()).isTrue();
        assertThat(usuarioService.findByEmail("admin@ua.es").isAdministrador()).isTrue();
        assertThat(usuarioService.existeAdministrador()).isTrue();

        UsuarioData normal = new UsuarioData();
        normal.setEmail("normal@ua.es");
        normal.setPassword("456");
        assertThat(usuarioService.registrar(normal).isAdministrador()).isFalse();
        assertThat(usuarioService.findAll()).hasSize(3);
    }

    @Test
    public void registroSegundoAdministradorSeRechazaSinCrearUsuario() {
        UsuarioData primero = new UsuarioData();
        primero.setEmail("admin@ua.es");
        primero.setPassword("123");
        primero.setAdministrador(true);
        usuarioService.registrar(primero);

        UsuarioData segundo = new UsuarioData();
        segundo.setEmail("otro.admin@ua.es");
        segundo.setPassword("456");
        segundo.setAdministrador(true);

        assertThatThrownBy(() -> usuarioService.registrar(segundo))
                .isInstanceOf(UsuarioServiceException.class)
                .hasMessage("Ya existe un usuario administrador");
        assertThat(usuarioService.findByEmail(segundo.getEmail())).isNull();
        assertThat(usuarioService.findAll()).hasSize(1);
    }

    @Test
    public void servicioListadoUsuariosDevuelveIdentificadoresYCorreos() {
        // GIVEN
        Long primerUsuarioId = addUsuarioBD();
        UsuarioData segundoUsuario = new UsuarioData();
        segundoUsuario.setEmail("otra.persona@ua");
        segundoUsuario.setPassword("456");
        UsuarioData segundoUsuarioRegistrado = usuarioService.registrar(segundoUsuario);

        // WHEN
        List<UsuarioData> usuarios = usuarioService.findAll();

        // THEN
        assertThat(usuarios).extracting("id", "email")
                .containsExactlyInAnyOrder(
                        tuple(primerUsuarioId, "user@ua"),
                        tuple(segundoUsuarioRegistrado.getId(), "otra.persona@ua"));
    }

    @Test
    public void servicioListadoUsuariosSinUsuariosDevuelveListaVacia() {
        assertThat(usuarioService.findAll()).isEmpty();
    }

    @Test
    public void servicioLoginUsuario() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // WHEN
        // intentamos logear un usuario y contraseña correctos
        UsuarioService.LoginStatus loginStatus1 = usuarioService.login("user@ua", "123");

        // intentamos logear un usuario correcto, con una contraseña incorrecta
        UsuarioService.LoginStatus loginStatus2 = usuarioService.login("user@ua", "000");

        // intentamos logear un usuario que no existe,
        UsuarioService.LoginStatus loginStatus3 = usuarioService.login("pepito.perez@gmail.com", "12345678");

        // THEN

        // el valor devuelto por el primer login es LOGIN_OK,
        assertThat(loginStatus1).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);

        // el valor devuelto por el segundo login es ERROR_PASSWORD,
        assertThat(loginStatus2).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // y el valor devuelto por el tercer login es USER_NOT_FOUND.
        assertThat(loginStatus3).isEqualTo(UsuarioService.LoginStatus.USER_NOT_FOUND);
    }

    @Test
    public void servicioRegistroUsuario() {
        // WHEN
        // Registramos un usuario con un e-mail no existente en la base de datos,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba2@gmail.com");
        usuario.setPassword("12345678");

        usuarioService.registrar(usuario);

        // THEN
        // el usuario se añade correctamente al sistema.

        UsuarioData usuarioBaseDatos = usuarioService.findByEmail("usuario.prueba2@gmail.com");
        assertThat(usuarioBaseDatos).isNotNull();
        assertThat(usuarioBaseDatos.getEmail()).isEqualTo("usuario.prueba2@gmail.com");
    }

    @Test
    public void servicioRegistroUsuarioExcepcionConNullPassword() {
        // WHEN, THEN
        // Si intentamos registrar un usuario con un password null,
        // se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }


    @Test
    public void servicioRegistroUsuarioExcepcionConEmailRepetido() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // THEN
        // Si registramos un usuario con un e-mail ya existente en la base de datos,
        // , se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("12345678");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }

    @Test
    public void servicioRegistroUsuarioDevuelveUsuarioConId() {

        // WHEN
        // Si registramos en el sistema un usuario con un e-mail no existente en la base de datos,
        // y un password no nulo,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");
        usuario.setPassword("12345678");

        UsuarioData usuarioNuevo = usuarioService.registrar(usuario);

        // THEN
        // se actualiza el identificador del usuario

        assertThat(usuarioNuevo.getId()).isNotNull();

        // con el identificador que se ha guardado en la BD.

        UsuarioData usuarioBD = usuarioService.findById(usuarioNuevo.getId());
        assertThat(usuarioBD).isEqualTo(usuarioNuevo);
    }

    @Test
    public void servicioConsultaUsuarioDevuelveUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos un usuario usando su e-mail,

        UsuarioData usuario = usuarioService.findByEmail("user@ua");

        // THEN
        // el usuario obtenido es el correcto.

        assertThat(usuario.getId()).isEqualTo(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("user@ua");
        assertThat(usuario.getNombre()).isEqualTo("Usuario Ejemplo");
    }
}
