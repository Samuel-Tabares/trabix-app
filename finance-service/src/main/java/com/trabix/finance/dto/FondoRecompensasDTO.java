package com.trabix.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs para el Fondo de Recompensas.
 */
public class FondoRecompensasDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaldoResponse {
        private BigDecimal saldoActual;
        private BigDecimal totalIngresos;
        private BigDecimal totalEgresos;
        private Long totalMovimientos;
        private LocalDateTime ultimaActualizacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoRequest {
        
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;
        
        @NotBlank(message = "La descripción es requerida")
        private String descripcion;
        
        /** Referencia opcional (ID de lote o cuadre) */
        private Long referenciaId;
        
        /** Tipo de referencia: CUADRE, LOTE, MANUAL */
        private String referenciaTipo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetiroRequest {
        
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;
        
        @NotBlank(message = "La descripción es requerida")
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PremioRequest {
        
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;
        
        @NotNull(message = "El beneficiario es requerido")
        private Long beneficiarioId;
        
        @NotBlank(message = "La descripción es requerida")
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenPeriodo {
        private LocalDateTime desde;
        private LocalDateTime hasta;
        private BigDecimal ingresos;
        private BigDecimal egresos;
        private BigDecimal balance;
        private Long cantidadMovimientos;
        private Long premiosEntregados;
    }
}
