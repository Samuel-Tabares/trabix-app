package com.trabix.equipment.dto;

import com.trabix.equipment.entity.EstadoAsignacion;
import com.trabix.equipment.entity.MotivoCancelacion;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Asignación de Equipos (Kit = Nevera + Pijama).
 */
public class AsignacionEquipoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long usuarioId;
        private String usuarioNombre;
        private String usuarioCedula;
        private LocalDateTime fechaInicio;
        private Integer diaCobroMensual;
        private EstadoAsignacion estado;
        private String estadoDescripcion;
        private String numeroSerieNevera;
        private String numeroSeriePijama;
        private String descripcion;
        private BigDecimal costoReposicionNevera;
        private BigDecimal costoReposicionPijama;
        private BigDecimal costoReposicionTotal;
        private LocalDateTime fechaFinalizacion;
        private MotivoCancelacion motivoCancelacion;
        private String motivoCancelacionDescripcion;
        private String notaFinalizacion;
        private Boolean reposicionPagada;
        private BigDecimal costoReposicionPendiente;
        private int pagosPendientes;
        private BigDecimal montoPendiente;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "El usuario es requerido")
        private Long usuarioId;
        
        @Size(max = 50, message = "El número de serie de nevera no puede exceder 50 caracteres")
        private String numeroSerieNevera;
        
        @Size(max = 50, message = "El número de serie de pijama no puede exceder 50 caracteres")
        private String numeroSeriePijama;
        
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String descripcion;
        
        /**
         * Día del mes para cobro (1-28). Si no se especifica, se usa el día actual.
         */
        @Min(value = 1, message = "El día de cobro debe ser entre 1 y 28")
        @Max(value = 28, message = "El día de cobro debe ser entre 1 y 28")
        private Integer diaCobroMensual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 50, message = "El número de serie de nevera no puede exceder 50 caracteres")
        private String numeroSerieNevera;
        
        @Size(max = 50, message = "El número de serie de pijama no puede exceder 50 caracteres")
        private String numeroSeriePijama;
        
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelarRequest {
        
        @NotNull(message = "El motivo de cancelación es requerido")
        private MotivoCancelacion motivo;
        
        @Size(max = 500, message = "La nota no puede exceder 500 caracteres")
        private String nota;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> asignaciones;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenUsuario {
        private Long usuarioId;
        private String nombre;
        private String cedula;
        private boolean tieneKitActivo;
        private Long asignacionActivaId;
        private int pagosPendientes;
        private BigDecimal totalPendiente;
        private int pagosVencidos;
        private boolean bloqueadoPorPagos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        private int stockDisponible;
        private long asignacionesActivas;
        private long asignacionesDevueltas;
        private long asignacionesCanceladas;
        private long pagosPendientes;
        private long pagosVencidos;
        private BigDecimal montoPendiente;
        private BigDecimal montoPagadoTotal;
        private long canceladasPendientesReposicion;
    }
}
