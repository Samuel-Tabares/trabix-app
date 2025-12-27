package com.trabix.finance.dto;

import com.trabix.finance.entity.ReferenciaMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
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

    /**
     * Request para ingreso manual al fondo.
     * Usado por ADMIN para registrar pagos de lotes de vendedores.
     */
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
        
        /**
         * ID del vendedor que pagó el lote (obligatorio para ingresos por pago de lote).
         */
        private Long vendedorId;
        
        /**
         * Cantidad de TRABIX del lote pagado.
         */
        @Min(value = 1, message = "La cantidad de TRABIX debe ser mayor a 0")
        private Integer cantidadTrabix;
        
        /**
         * ID de referencia externa (lote, cuadre, etc.)
         */
        private Long referenciaId;
        
        /**
         * Tipo de referencia.
         */
        private ReferenciaMovimiento referenciaTipo;
    }

    /**
     * Request para retiro del fondo.
     */
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

    /**
     * Request para entregar premio a un beneficiario.
     */
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
        
        /**
         * Tipo de premio (PREMIO, INCENTIVO, BONIFICACION).
         * Por defecto: PREMIO
         */
        private ReferenciaMovimiento tipoPremio;
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
        private Long pagosLoteRecibidos;
    }
}
