package com.petsalud.backendpetsalud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuarioQueIntentaSerAdmin(String password) {
        return Usuario.builder()
                .email("ana@test.cl")
                .password(password)
                .nombre("Ana")
                .apellido("Pérez")
                .direccion("Av. Siempre Viva 123")
                .rol(Rol.ADMIN)
                .build();
    }

    @Test
    void registrarUsuario_emailDuplicado_lanzaError() {
        // Arrange
        when(usuarioRepository.findByEmail("ana@test.cl"))
                .thenReturn(Optional.of(usuarioQueIntentaSerAdmin("Clave123!")));

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin("Clave123!")));

        // Assert
        assertEquals("El email ya está registrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarUsuario_forzaElRolCliente() {
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin("Clave123!"));

        assertEquals(Rol.CLIENTE, resultado.getRol());
    }

    @Test
    void registrarUsuario_encriptaLaContrasena() {
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin("Clave123!"));

        assertEquals("hash-bcrypt", resultado.getPassword());
        verify(passwordEncoder).encode("Clave123!");
    }

    @Test
    void registrarUsuario_contrasenaYaEncriptada2a_noLaVuelveAEncriptar() {
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin("$2a$10$abcdefghijk"));

        assertEquals("$2a$10$abcdefghijk", resultado.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registrarUsuario_contrasenaYaEncriptada2b_noLaVuelveAEncriptar() {
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin("$2b$10$abcdefghijk"));

        assertEquals("$2b$10$abcdefghijk", resultado.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registrarUsuario_sinContrasena_noIntentaEncriptar() {
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.registrarUsuario(usuarioQueIntentaSerAdmin(null));

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizarUsuario_existente_actualizaDatosPeroNoElEmail() {
        Usuario existente = Usuario.builder()
                .id(1L).email("ana@test.cl").nombre("Ana").apellido("Pérez")
                .telefono("111").direccion("Calle Vieja 1").rol(Rol.CLIENTE)
                .build();
        Usuario cambios = Usuario.builder()
                .email("otro@test.cl").nombre("Ana María").apellido("Soto")
                .telefono("222").direccion("Calle Nueva 2")
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.actualizarUsuario(1L, cambios);

        assertEquals("Ana María", resultado.getNombre());
        assertEquals("Soto", resultado.getApellido());
        assertEquals("222", resultado.getTelefono());
        assertEquals("Calle Nueva 2", resultado.getDireccion());
        assertEquals("ana@test.cl", resultado.getEmail());
    }

    @Test
    void actualizarUsuario_inexistente_lanzaError() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(99L, Usuario.builder().build()));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioPorId_devuelveElUsuario() {
        Usuario usuario = Usuario.builder().id(1L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.obtenerUsuarioPorId(1L);

        assertSame(usuario, resultado.get());
    }

    @Test
    void obtenerUsuarioPorEmail_devuelveElUsuario() {
        Usuario usuario = Usuario.builder().id(1L).email("ana@test.cl").build();
        when(usuarioRepository.findByEmail("ana@test.cl")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.obtenerUsuarioPorEmail("ana@test.cl");

        assertSame(usuario, resultado.get());
    }

    @Test
    void eliminarUsuario_llamaAlRepositorio() {
        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void listarUsuarios_devuelveTodosLosUsuarios() {
        when(usuarioRepository.findAll())
                .thenReturn(List.of(Usuario.builder().id(1L).build(), Usuario.builder().id(2L).build()));

        assertEquals(2, usuarioService.listarUsuarios().size());
    }
}