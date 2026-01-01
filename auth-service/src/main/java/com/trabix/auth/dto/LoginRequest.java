package com.trabix.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = "^[0-9]+$", message = "La cédula debe contener solo números")
    @Size(min = 6, max = 15, message = "La cédula debe tener entre 6 y 15 dígitos")
    private String cedula;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
}
