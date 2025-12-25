package com.trabix.equipment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Equipos.
 */
public class EquipoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long usuarioId;
        private String usuarioNombre;
        private String usuarioCedula;
        private String tipo;
        private LocalDateTime fechaInicio;
        private String estado;
        private BigDecimal costoReposicion;
        private String numeroSerie;
        private String descripcion;
        private LocalDateTime fechaDevolucion;
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
        
        @NotBlank(message = "El tipo es requerido")
        @Pattern(regexp = "NEVERA|PIJAMA", message = "Tipo inválido. Use: NEVERA o PIJAMA")
        private String tipo;
        
        @DecimalMin(value = "0.01", message = "El costo de reposición debe ser mayor a 0")
        private BigDecimal costoReposicion;
        
        @Size(max = 50, message = "El número de serie no puede exceder 50 caracteres")
        private String numeroSerie;
        
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 50, message = "El número de serie no puede exceder 50 caracteres")
        private String numeroSerie;
        
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        private String descripcion;
        
        @DecimalMin(value = "0.01", message = "El costo de reposición debe ser mayor a 0")
        private BigDecimal costoReposicion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> equipos;
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
        private int equiposActivos;
        private int neveras;
        private int pijamas;
        private int pagosPendientes;
        private BigDecimal totalPendiente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        private long totalEquipos;
        private long equiposActivos;
        private long equiposDevueltos;
        private long equiposPerdidos;
        private long totalNeveras;
        private long totalPijamas;
        private long pagosPendientes;
        private BigDecimal montoPendiente;
        private BigDecimal montoPagado;
    }
}
