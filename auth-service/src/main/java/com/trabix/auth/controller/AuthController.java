package com.trabix.auth.controller;

import com.trabix.auth.dto.*;
import com.trabix.auth.entity.Usuario;
import com.trabix.auth.service.AuthService;
import com.trabix.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación.
 * 
 * Endpoints:
 * - POST /auth/login - Iniciar sesión
 * - POST /auth/refresh - Renovar token
 * - POST /auth/logout - Cerrar sesión (todas las sesiones)
 * - POST /auth/logout/token - Cerrar sesión específica
 * - POST /auth/cambiar-password - Cambiar contraseña
 * - GET /auth/me - Obtener perfil del usuario autenticado
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación y gestión de sesión")
public class AuthController {

    private final AuthService authService;

    /**
     * Iniciar sesión con cédula y contraseña.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y retorna tokens JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login exitoso"));
    }

    /**
     * Renovar access token usando refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Genera nuevo access token usando refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token renovado"));
    }

    /**
     * Cerrar todas las sesiones del usuario.
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca todos los refresh tokens del usuario")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Usuario usuario) {
        authService.logout(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada correctamente"));
    }

    /**
     * Cerrar una sesión específica (por refresh token).
     */
    @PostMapping("/logout/token")
    @Operation(summary = "Cerrar sesión específica", description = "Revoca un refresh token específico")
    public ResponseEntity<ApiResponse<Void>> logoutToken(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logoutToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Sesión específica cerrada"));
    }

    /**
     * Cambiar contraseña del usuario autenticado.
     */
    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(usuario.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña cambiada exitosamente. Por favor inicie sesión nuevamente."));
    }

    /**
     * Obtener información del usuario autenticado.
     */
    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Obtiene información del usuario autenticado")
    public ResponseEntity<ApiResponse<AuthResponse.UsuarioInfo>> me(@AuthenticationPrincipal Usuario usuario) {
        AuthResponse.UsuarioInfo info = AuthResponse.UsuarioInfo.builder()
                .id(usuario.getId())
                .cedula(usuario.getCedula())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .nivel(usuario.getNivel())
                .telefono(usuario.getTelefono())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
