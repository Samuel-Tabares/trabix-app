package com.trabix.auth.controller;

import com.trabix.auth.dto.AuthResponse;
import com.trabix.auth.dto.LoginRequest;
import com.trabix.auth.dto.RefreshTokenRequest;
import com.trabix.auth.entity.Usuario;
import com.trabix.auth.service.AuthService;
import com.trabix.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación.
 * Endpoints: /auth/login, /auth/refresh, /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación y gestión de sesión")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y retorna tokens JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login exitoso"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Genera nuevo access token usando refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token renovado"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca todos los refresh tokens del usuario")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Usuario usuario) {
        authService.logout(usuario.getId());
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada correctamente"));
    }

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
                .build();
        
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
