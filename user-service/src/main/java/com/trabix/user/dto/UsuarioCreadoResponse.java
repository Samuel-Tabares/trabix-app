package com.trabix.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al crear un usuario.
 * Incluye la contraseña generada (solo se muestra una vez).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreadoResponse {

    private UsuarioResponse usuario;
    
    /**
     * Contraseña generada automáticamente.
     * ⚠️ IMPORTANTE: Solo se muestra una vez, guardarla inmediatamente.
     */
    private String passwordGenerado;
    
    /**
     * Mensaje de texto listo para enviar por WhatsApp al nuevo vendedor.
     */
    private String mensajeWhatsApp;
}
