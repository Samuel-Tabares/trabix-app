package com.trabix.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta con datos de un usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResponse {

    private Long id;
    private String cedula;
    private String nombre;
    private String telefono;
    private String correo;
    private RolUsuario rol;
    private String nivel;
    private String modeloNegocio;
    private EstadoUsuario estado;
    private LocalDateTime fechaIngreso;
    
    // Info del reclutador (simplificada)
    private ReclutadorInfo reclutador;
    
    // Estadísticas básicas
    private Integer totalReclutados;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReclutadorInfo {
        private Long id;
        private String nombre;
        private String nivel;
        private String telefono;
        private String correo;
    }
}
