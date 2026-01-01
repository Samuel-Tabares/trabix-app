package com.trabix.auth.service;

import com.trabix.auth.dto.*;
import com.trabix.auth.entity.RefreshToken;
import com.trabix.auth.entity.Usuario;
import com.trabix.auth.repository.RefreshTokenRepository;
import com.trabix.auth.repository.UsuarioRepository;
import com.trabix.auth.security.JwtService;
import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.exception.CredencialesInvalidasException;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación: login, logout, refresh token, cambio de contraseña.
 * 
 * SEGURIDAD:
 * - Protección contra fuerza bruta (bloqueo temporal)
 * - Máximo 5 intentos fallidos
 * - Bloqueo de 15 minutos después de exceder intentos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${trabix.security.max-intentos-login:5}")
    private int maxIntentosLogin;

    @Value("${trabix.security.bloqueo-minutos:15}")
    private int bloqueoMinutos;

    @Value("${trabix.security.max-sesiones-activas:5}")
    private int maxSesionesActivas;

    /**
     * Autentica un usuario y genera tokens.
     * 
     * SEGURIDAD: Implementa protección contra fuerza bruta.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String cedula = request.getCedula();
        
        // 1. Verificar si el usuario existe
        Usuario usuario = usuarioRepository.findByCedula(cedula).orElse(null);
        
        if (usuario == null) {
            log.warn("Intento de login con cédula inexistente: {}", cedula);
            throw new CredencialesInvalidasException();
        }

        // 2. Verificar si está bloqueado
        if (usuario.estaBloqueado()) {
            log.warn("Intento de login con cuenta bloqueada: {}", cedula);
            throw new ValidacionNegocioException(
                    String.format("Cuenta bloqueada temporalmente. Intente nuevamente en %d minutos.", 
                            bloqueoMinutos));
        }

        // 3. Verificar estado activo
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            log.warn("Intento de login con cuenta inactiva: {}", cedula);
            throw new CredencialesInvalidasException("Cuenta inactiva o suspendida");
        }

        // 4. Intentar autenticación
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cedula, request.getPassword())
            );
        } catch (AuthenticationException e) {
            // Incrementar intentos fallidos
            manejarIntentoFallido(usuario);
            throw new CredencialesInvalidasException();
        }

        // 5. Login exitoso - resetear intentos y registrar
        usuario.registrarLoginExitoso();
        usuarioRepository.save(usuario);

        // 6. Limitar sesiones activas (revocar las más antiguas si excede)
        limitarSesionesActivas(usuario);

        // 7. Generar tokens
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = jwtService.generarRefreshToken(usuario);

        // 8. Guardar refresh token con info del dispositivo
        guardarRefreshToken(usuario, refreshToken, httpRequest);

        log.info("Login exitoso: {} - Rol: {} - IP: {}", 
                usuario.getCedula(), usuario.getRol(), obtenerIp(httpRequest));

        return construirAuthResponse(usuario, accessToken, refreshToken);
    }

    /**
     * Versión simplificada de login (sin HttpServletRequest).
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    /**
     * Maneja un intento de login fallido.
     */
    private void manejarIntentoFallido(Usuario usuario) {
        usuario.incrementarIntentosFallidos();
        
        if (usuario.getIntentosFallidos() >= maxIntentosLogin) {
            usuario.bloquearCuenta(bloqueoMinutos);
            log.warn("Cuenta bloqueada por exceso de intentos: {}", usuario.getCedula());
        }
        
        usuarioRepository.save(usuario);
        log.warn("Intento de login fallido #{} para: {}", 
                usuario.getIntentosFallidos(), usuario.getCedula());
    }

    /**
     * Limita el número de sesiones activas por usuario.
     */
    private void limitarSesionesActivas(Usuario usuario) {
        long sesionesActivas = refreshTokenRepository.contarTokensActivos(
                usuario.getId(), LocalDateTime.now());
        
        if (sesionesActivas >= maxSesionesActivas) {
            // Revocar todos los tokens anteriores
            refreshTokenRepository.revocarTodosDelUsuario(usuario);
            log.info("Sesiones anteriores revocadas para: {} (excedió límite de {})", 
                    usuario.getCedula(), maxSesionesActivas);
        }
    }

    /**
     * Renueva el access token usando el refresh token.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(request.getRefreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new CredencialesInvalidasException("Token de refresh inválido o expirado"));

        Usuario usuario = refreshToken.getUsuario();

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new CredencialesInvalidasException("Usuario inactivo");
        }

        if (usuario.estaBloqueado()) {
            throw new CredencialesInvalidasException("Cuenta bloqueada temporalmente");
        }

        // Generar nuevo access token (mantener mismo refresh token)
        String nuevoAccessToken = jwtService.generarAccessToken(usuario);

        log.debug("Token renovado para: {}", usuario.getCedula());

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
     * Cierra sesión específica (un solo refresh token).
     */
    @Transactional
    public void logoutToken(String refreshToken) {
        refreshTokenRepository.revocarToken(refreshToken);
        log.debug("Token específico revocado");
    }

    /**
     * Cambia la contraseña del usuario.
     */
    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest request) {
        // Validar que las contraseñas coincidan
        if (!request.getPasswordNuevo().equals(request.getPasswordConfirmacion())) {
            throw new ValidacionNegocioException("Las contraseñas no coinciden");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        // Validar contraseña actual
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Contraseña actual incorrecta");
        }

        // Validar que la nueva contraseña sea diferente
        if (passwordEncoder.matches(request.getPasswordNuevo(), usuario.getPasswordHash())) {
            throw new ValidacionNegocioException("La nueva contraseña debe ser diferente a la actual");
        }

        // Actualizar contraseña
        String nuevoHash = passwordEncoder.encode(request.getPasswordNuevo());
        usuarioRepository.actualizarPassword(usuarioId, nuevoHash, LocalDateTime.now());

        // Revocar todos los tokens (forzar re-login)
        refreshTokenRepository.revocarTodosDelUsuario(usuario);

        log.info("Contraseña cambiada para usuario: {}", usuario.getCedula());
    }

    /**
     * Guarda un nuevo refresh token.
     */
    private void guardarRefreshToken(Usuario usuario, String token, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(token)
                .fechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .revocado(false)
                .ipAddress(request != null ? obtenerIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Obtiene la IP del cliente.
     */
    private String obtenerIp(HttpServletRequest request) {
        if (request == null) return null;
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Si hay múltiples IPs (proxy), tomar la primera
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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
                        .telefono(usuario.getTelefono())
                        .build())
                .build();
    }
}
