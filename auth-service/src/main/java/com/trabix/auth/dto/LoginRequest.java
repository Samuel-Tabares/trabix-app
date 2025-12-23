package com.trabix.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para iniciar sesión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "La cédula es requerida")
    private String cedula;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
