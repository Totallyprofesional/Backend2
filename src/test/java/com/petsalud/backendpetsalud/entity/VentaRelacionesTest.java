package com.petsalud.backendpetsalud.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class VentaRelacionesTest {

    private Usuario crearUsuario() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Pérez")
                .email("ana@test.cl")
                .direccion("Av. Siempre Viva 123")
                .rol(Rol.CLIENTE)
                .build();
    }

    private Producto crearProducto(Long id, String nombre, String precio) {
        return Producto.builder()
                .id(id)
                .nombre(nombre)
                .precio(new BigDecimal(precio))
                .stock(5)
                .build();
    }

    @Test
    void venta_quedaVinculadaAlUsuarioQueLaRealizo() {
        Usuario usuario = crearUsuario();

        Venta venta = Venta.builder().usuario(usuario).build();

        assertSame(usuario, venta.getUsuario());
        assertEquals("ana@test.cl", venta.getUsuario().getEmail());
    }

    @Test
    void usuario_conoceLasVentasQueRealizo() {
        Usuario usuario = crearUsuario();
        Venta venta = Venta.builder().usuario(usuario).build();

        usuario.setVentas(List.of(venta));

        assertEquals(1, usuario.getVentas().size());
        assertSame(usuario, usuario.getVentas().get(0).getUsuario());
    }

    @Test
    void venta_contieneLosProductosVendidos() {
        Producto shampoo = crearProducto(10L, "Shampoo", "8990");
        Producto collar = crearProducto(11L, "Collar", "5990");

        Venta venta = Venta.builder().productos(List.of(shampoo, collar)).build();

        assertEquals(2, venta.getProductos().size());
        assertTrue(venta.getProductos().contains(shampoo));
        assertTrue(venta.getProductos().contains(collar));
    }
}
