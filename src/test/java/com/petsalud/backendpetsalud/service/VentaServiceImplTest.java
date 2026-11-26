package com.petsalud.backendpetsalud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

        @Mock
        private VentaRepository ventaRepository;

        @Mock
        private UsuarioRepository usuarioRepository;

        @Mock
        private ProductoRepository productoRepository;

        @InjectMocks
        private VentaServiceImpl ventaService;

        private Usuario usuarioCompleto() {
                return Usuario.builder()
                                .id(1L)
                                .nombre("Lorena")
                                .apellido("Dias")
                                .email("lorena@test.cl")
                                .direccion("Calle Nueva 5555")
                                .rol(Rol.CLIENTE)
                                .build();
        }

        @Test
        void realizarVenta_usuarioSinNombre_lanzaError() {
                // Arrange
                Usuario usuario = usuarioCompleto();
                usuario.setNombre(null);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                // Act
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                // Assert
                assertEquals("El usuario no tiene nombre registrado", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_usuarioSinApellido_lanzaError() {
                Usuario usuario = usuarioCompleto();
                usuario.setApellido(null);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                assertEquals("El usuario no tiene apellido registrado", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_usuarioSinEmail_lanzaError() {
                Usuario usuario = usuarioCompleto();
                usuario.setEmail("");
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                assertEquals("El usuario no tiene email registrado", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_usuarioConDireccionEnBlanco_lanzaError() {
                Usuario usuario = usuarioCompleto();
                usuario.setDireccion("   ");
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                assertEquals("El usuario no tiene dirección registrada", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_productoSinStock_lanzaErrorYNoGuardaVenta() {
                // Arrange
                Usuario usuario = usuarioCompleto();
                Producto producto = Producto.builder()
                                .id(10L).nombre("Alimento premium").precio(new BigDecimal("25990")).stock(0)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(producto));

                // Act
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                // Assert
                assertEquals("El producto Alimento premium no tiene stock disponible", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_unProductoSinStock_noDescuentaStockDeLosDemas() {
                Usuario usuario = usuarioCompleto();
                Producto conStock = Producto.builder()
                                .id(10L).nombre("Shampoo").precio(new BigDecimal("8990")).stock(5)
                                .build();
                Producto sinStock = Producto.builder()
                                .id(11L).nombre("Collar").precio(new BigDecimal("5990")).stock(0)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(conStock, sinStock));

                assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

                assertEquals(5, conStock.getStock().intValue());
                verify(productoRepository, never()).save(any());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_usuarioVeterinario_noTienePermiso() {
                Usuario usuario = usuarioCompleto();
                usuario.setRol(Rol.VETERINARIO);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                assertEquals("El rol del usuario no está autorizado para realizar ventas", ex.getMessage());
                verify(ventaRepository, never()).save(any());
        }

        @Test
        void realizarVenta_calculaElTotalSumandoLosPrecios() {
                // Arrange
                Usuario usuario = usuarioCompleto();
                Producto shampoo = Producto.builder()
                                .id(10L).nombre("Shampoo").precio(new BigDecimal("8990")).stock(5)
                                .build();
                Producto collar = Producto.builder()
                                .id(11L).nombre("Collar").precio(new BigDecimal("5990")).stock(2)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shampoo, collar));
                when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

                // Assert
                assertEquals(new BigDecimal("14980"), venta.getTotal());
        }

        @Test
        void realizarVenta_descuentaUnaUnidadDeStockPorProducto() {
                Usuario usuario = usuarioCompleto();
                Producto shampoo = Producto.builder()
                                .id(10L).nombre("Shampoo").precio(new BigDecimal("8990")).stock(5)
                                .build();
                Producto collar = Producto.builder()
                                .id(11L).nombre("Collar").precio(new BigDecimal("5990")).stock(2)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shampoo, collar));
                when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

                ventaService.realizarVenta(1L, List.of(10L, 11L));

                assertEquals(4, shampoo.getStock().intValue());
                assertEquals(1, collar.getStock().intValue());
                verify(productoRepository).save(shampoo);
                verify(productoRepository).save(collar);
        }

        @Test
        void realizarVenta_usuarioAdminConDatosCompletos_registraLaVenta() {
                Usuario usuario = usuarioCompleto();
                usuario.setRol(Rol.ADMIN);
                Producto alimento = Producto.builder()
                                .id(10L).nombre("Alimento premium").precio(new BigDecimal("25990")).stock(3)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(alimento));
                when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

                Venta venta = ventaService.realizarVenta(1L, List.of(10L));

                assertNotNull(venta.getFecha());
                assertSame(usuario, venta.getUsuario());
                assertEquals(1, venta.getProductos().size());
                verify(ventaRepository, times(1)).save(any(Venta.class));
        }

        @Test
        void realizarVenta_usuarioInexistente_lanzaError() {
                when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(99L, List.of(10L)));

                assertEquals("Usuario no encontrado", ex.getMessage());
        }

        @Test
        void realizarVenta_productoInexistente_lanzaError() {
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCompleto()));
                when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of());

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

                assertEquals("Algunos productos no fueron encontrados", ex.getMessage());
        }

        @Test
        void realizarVenta_productoConStockNulo_seTrataComoSinStock() {
                Producto producto = Producto.builder()
                                .id(10L).nombre("Juguete").precio(new BigDecimal("3990")).stock(null)
                                .build();
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCompleto()));
                when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(producto));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                () -> ventaService.realizarVenta(1L, List.of(10L)));

                assertEquals("El producto Juguete no tiene stock disponible", ex.getMessage());
        }

        @Test
        void obtenerVentaPorId_existente_devuelveLaVenta() {
                Venta venta = Venta.builder().id(5L).build();
                when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

                Optional<Venta> resultado = ventaService.obtenerVentaPorId(5L);

                assertTrue(resultado.isPresent());
                assertSame(venta, resultado.get());
        }

        @Test
        void obtenerVentaPorId_inexistente_devuelveVacio() {
                when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

                assertTrue(ventaService.obtenerVentaPorId(99L).isEmpty());
        }

        @Test
        void listarVentasPorUsuario_devuelveLasVentasDelUsuario() {
                Venta v1 = Venta.builder().id(1L).build();
                Venta v2 = Venta.builder().id(2L).build();
                when(ventaRepository.findByUsuarioId(1L)).thenReturn(List.of(v1, v2));

                List<Venta> resultado = ventaService.listarVentasPorUsuario(1L);

                assertEquals(2, resultado.size());
        }

        @Test
        void listarTodasLasVentas_devuelveTodasLasVentas() {
                when(ventaRepository.findAll()).thenReturn(List.of(Venta.builder().id(1L).build()));

                assertEquals(1, ventaService.listarTodasLasVentas().size());
        }

}
