package com.trabix.equipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTOs para Stock de Equipos.
 */
public class StockEquiposDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer kitsDisponibles;
        private Integer totalKitsHistorico;
        private Long asignacionesActivas;
        private LocalDateTime ultimaActualizacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgregarKitsRequest {
        @NotNull(message = "La cantidad es requerida")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AjustarStockRequest {
        @NotNull(message = "El nuevo valor es requerido")
        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer nuevoValor;
    }
}
