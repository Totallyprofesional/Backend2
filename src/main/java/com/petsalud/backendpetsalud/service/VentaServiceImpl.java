package com.petsalud.backendpetsalud.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;

@Service
public class VentaServiceImpl implements VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public Venta realizarVenta(Long usuarioId, List<Long> productosIds) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        validarUsuario(usuario);

        List<Producto> productos = productoRepository.findAllById(productosIds);

        if (productos.size() != productosIds.size()) {
            throw new IllegalArgumentException("Algunos productos no fueron encontrados");
        }

        for (Producto producto : productos) {
            if (producto.getStock() == null || producto.getStock() <= 0) {
                throw new IllegalArgumentException(
                        "El producto " + producto.getNombre() + " no tiene stock disponible");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Producto producto : productos) {
            producto.setStock(producto.getStock() - 1);
            productoRepository.save(producto);
            total = total.add(producto.getPrecio());
        }

        Venta venta = Venta.builder()
                .usuario(usuario)
                .productos(productos)
                .total(total)
                .fecha(LocalDateTime.now())
                .build();

        return ventaRepository.save(venta);
    }

    @Override
    public Optional<Venta> obtenerVentaPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    public List<Venta> listarVentasPorUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Venta> listarTodasLasVentas() {
        return ventaRepository.findAll();
    }

    private void validarUsuario(Usuario usuario) {
        if (esVacio(usuario.getNombre())) {
            throw new IllegalArgumentException("El usuario no tiene nombre registrado");
        }
        if (esVacio(usuario.getApellido())) {
            throw new IllegalArgumentException("El usuario no tiene apellido registrado");
        }
        if (esVacio(usuario.getEmail())) {
            throw new IllegalArgumentException("El usuario no tiene email registrado");
        }
        if (esVacio(usuario.getDireccion())) {
            throw new IllegalArgumentException("El usuario no tiene dirección registrada");
        }
        if (usuario.getRol() != Rol.ADMIN && usuario.getRol() != Rol.CLIENTE) {
            throw new IllegalArgumentException("El rol del usuario no está autorizado para realizar ventas");
        }
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
