package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Logins {

    private static final Logger log = LoggerFactory.getLogger(Logins.class);
    private static final int MAX_INTENTOS = 5;
    private static final long MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;

    public Logins(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @EventListener
    public void onLoginFallido(AuthenticationFailureBadCredentialsEvent e) {
        String email = e.getAuthentication().getName();
        log.warn("LOGIN_FALLIDO usuario={}", email);
        usuarioRepository.findByEmail(email).ifPresent(u -> {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            if (u.getIntentosFallidos() >= MAX_INTENTOS) {
                u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                u.setIntentosFallidos(0);
                log.error("CUENTA_BLOQUEADA usuario={} hasta={}", email, u.getBloqueadoHasta());
            }
            usuarioRepository.save(u);
        });
    }

    @EventListener
    public void onLoginExitoso(AuthenticationSuccessEvent e) {
        String email = e.getAuthentication().getName();
        log.info("LOGIN_OK usuario={}", email);
        usuarioRepository.findByEmail(email).ifPresent(u -> {
            if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
                u.setIntentosFallidos(0);
                u.setBloqueadoHasta(null);
                usuarioRepository.save(u);
            }
        });
    }

    @EventListener
    public void onAccesoDenegado(AuthorizationDeniedEvent<?> e) {
        log.warn("ACCESO_DENEGADO usuario={}", e.getAuthentication().get().getName());
    }
}