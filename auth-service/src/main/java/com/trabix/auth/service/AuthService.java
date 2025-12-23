package com.trabix.auth.service;

import com.trabix.auth.dto.AuthResponse;
import com.trabix.auth.dto.LoginRequest;
import com.trabix.auth.dto.RefreshTokenRequest;
import com.trabix.auth.entity.RefreshToken;
import com.trabix.auth.entity.Usuario;
import com.trabix.auth.repository.RefreshTokenRepository;
import com.trabix.auth.repository.UsuarioRepository;
import com.trabix.auth.security.JwtService;
import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.exception.CredencialesInvalidasException;
import com.trabix.common.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación: login, logout, refresh token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Autentica un usuario y genera tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Autenticar con Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getCedula(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("Intento de login fallido para cédula: {}", request.getCedula());
            throw new CredencialesInvalidasException();
        }

        // Obtener usuario
        Usuario usuario = usuarioRepository
                .findByCedulaAndEstado(request.getCedula(), EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new CredencialesInvalidasException());

        // Revocar tokens anteriores
        refreshTokenRepository.revocarTodosDelUsuario(usuario);

        // Generar nuevos tokens
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = jwtService.generarRefreshToken(usuario);

        // Guardar refresh token
        guardarRefreshToken(usuario, refreshToken);

        log.info("Login exitoso: {} - Rol: {}", usuario.getCedula(), usuario.getRol());

        return construirAuthResponse(usuario, accessToken, refreshToken);
    }

    /**
     * Renueva el access token usando el refresh token.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new CredencialesInvalidasException("Token de refresh inválido"));

        if (!refreshToken.isValido()) {
            throw new CredencialesInvalidasException("Token de refresh expirado o revocado");
        }

        Usuario usuario = refreshToken.getUsuario();

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new CredencialesInvalidasException("Usuario inactivo");
        }

        // Generar nuevo access token (mantener mismo refresh token)
        String nuevoAccessToken = jwtService.generarAccessToken(usuario);

        log.info("Token renovado para: {}", usuario.getCedula());

        return construirAuthResponse(usuario, nuevoAccessToken, request.getRefreshToken());
    }

    /**
     * Cierra sesión revocando todos los refresh tokens del usuario.
     */
    @Transactional
    public void logout(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        refreshTokenRepository.revocarTodosDelUsuario(usuario);
        log.info("Logout: tokens revocados para usuario {}", usuario.getCedula());
    }

    /**
     * Guarda un nuevo refresh token.
     */
    private void guardarRefreshToken(Usuario usuario, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(token)
                .fechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .revocado(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Construye la respuesta de autenticación.
     */
    private AuthResponse construirAuthResponse(Usuario usuario, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .usuario(AuthResponse.UsuarioInfo.builder()
                        .id(usuario.getId())
                        .cedula(usuario.getCedula())
                        .nombre(usuario.getNombre())
                        .correo(usuario.getCorreo())
                        .rol(usuario.getRol())
                        .nivel(usuario.getNivel())
                        .build())
                .build();
    }
}
