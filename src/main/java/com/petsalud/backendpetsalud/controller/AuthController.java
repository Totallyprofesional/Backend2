package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.AuthResponse;
import com.petsalud.backendpetsalud.dto.LoginRequest;
import com.petsalud.backendpetsalud.dto.RegistroRequest;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import com.petsalud.backendpetsalud.security.jwt.JwtService;
import com.petsalud.backendpetsalud.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@RequestBody RegistroRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder().mensaje("Email y password son obligatorios").build());
        }

        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .password(request.getPassword()) 
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .build(); 

        try {
            Usuario guardado = usuarioService.registrarUsuario(usuario);
            UsuarioDetails userDetails = new UsuarioDetails(guardado);
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(AuthResponse.builder() 
                    .token(token)
                    .email(guardado.getEmail())
                    .rol(guardado.getRol().name())
                    .mensaje("Usuario registrado exitosamente")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder().mensaje(e.getMessage()).build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            UsuarioDetails userDetails = (UsuarioDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .email(userDetails.getUsername())
                    .rol(userDetails.getAuthorities().iterator().next().getAuthority())
                    .mensaje("Autenticación exitosa")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    AuthResponse.builder().mensaje("Credenciales inválidas").build());
        }
    }
}