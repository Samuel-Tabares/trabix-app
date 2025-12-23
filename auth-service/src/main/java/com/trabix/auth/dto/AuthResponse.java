package com.trabix.auth.dto;

import com.trabix.common.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de autenticación exitosa con tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tipo;
    private Long expiresIn;
    private UsuarioInfo usuario;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfo {
        private Long id;
        private String cedula;
        private String nombre;
        private String correo;
        private RolUsuario rol;
        private String nivel;
    }
}
