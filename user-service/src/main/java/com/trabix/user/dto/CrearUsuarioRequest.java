package com.trabix.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear un nuevo usuario/vendedor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearUsuarioRequest {

    @NotBlank(message = "La cédula es requerida")
    @Size(min = 6, max = 20, message = "La cédula debe tener entre 6 y 20 caracteres")
    private String cedula;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "El correo debe ser válido")
    private String correo;

    /**
     * ID del reclutador (quien lo trae al negocio).
     * Si es null, entra directamente con el admin (N2, modelo 60/40).
     * Si tiene reclutador, entra un nivel abajo (N3+, modelo 50/50).
     */
    private Long reclutadorId;

    /**
     * Contraseña opcional. Si no se proporciona, se genera automáticamente.
     */
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres")
    private String password;
}
