package com.petsalud.backendpetsalud.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;

@Component
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public DatosIniciales(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("admin@petsalud.cl", "Admin1234!", "Admin", Rol.ADMIN);
        crear("vet@petsalud.cl", "Vet12345!", "Veterinario", Rol.VETERINARIO);
    }

    private void crear(String email, String clave, String nombre, Rol rol) {
        if (repo.findByEmail(email).isEmpty()) {
            repo.save(Usuario.builder()
                    .email(email).password(encoder.encode(clave))
                    .nombre(nombre).rol(rol).build());
        }
    }
}