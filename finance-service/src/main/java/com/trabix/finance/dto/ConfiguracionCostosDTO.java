package com.trabix.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para configuración de costos.
 */
public class ConfiguracionCostosDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private BigDecimal costoRealTrabix;
        private BigDecimal costoPercibidoTrabix;
        private BigDecimal aporteFondoPorTrabix;
        private BigDecimal aporteGestionPorTrabix;
        private BigDecimal diferenciaCosto;
        private BigDecimal margenTotalPorTrabix;
        private LocalDateTime fechaActualizacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @NotNull(message = "El costo real es requerido")
        @DecimalMin(value = "0.01", message = "El costo real debe ser mayor a 0")
        private BigDecimal costoRealTrabix;
        
        @NotNull(message = "El costo percibido es requerido")
        @DecimalMin(value = "0.01", message = "El costo percibido debe ser mayor a 0")
        private BigDecimal costoPercibidoTrabix;
        
        @NotNull(message = "El aporte al fondo es requerido")
        @DecimalMin(value = "0", message = "El aporte al fondo no puede ser negativo")
        private BigDecimal aporteFondoPorTrabix;
        
        @NotNull(message = "El aporte de gestión es requerido")
        @DecimalMin(value = "0", message = "El aporte de gestión no puede ser negativo")
        private BigDecimal aporteGestionPorTrabix;
    }

    /**
     * Vista simplificada para vendedores (solo ven costo percibido).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorView {
        private BigDecimal costoPorTrabix;
        private LocalDateTime fechaActualizacion;
    }
}
