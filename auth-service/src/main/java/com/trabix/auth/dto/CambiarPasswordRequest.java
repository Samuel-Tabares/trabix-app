package com.trabix.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para cambiar contraseña.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
    )
    private String passwordNuevo;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String passwordConfirmacion;
}
