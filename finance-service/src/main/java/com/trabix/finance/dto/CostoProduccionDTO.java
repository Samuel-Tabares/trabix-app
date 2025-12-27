package com.trabix.finance.dto;

import com.trabix.finance.entity.TipoCosto;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Costos de Producción.
 */
public class CostoProduccionDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String concepto;
        private Integer cantidad;
        private BigDecimal costoUnitario;
        private BigDecimal costoTotal;
        private TipoCosto tipo;
        private String tipoDescripcion;
        private LocalDateTime fecha;
        private String nota;
        private String proveedor;
        private String numeroFactura;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "El concepto es requerido")
        @Size(max = 100, message = "El concepto no puede exceder 100 caracteres")
        private String concepto;
        
        @NotNull(message = "La cantidad es requerida")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;
        
        @NotNull(message = "El costo unitario es requerido")
        @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a 0")
        private BigDecimal costoUnitario;
        
        @NotNull(message = "El tipo es requerido")
        private TipoCosto tipo;
        
        private LocalDateTime fecha;
        
        @Size(max = 500, message = "La nota no puede exceder 500 caracteres")
        private String nota;
        
        @Size(max = 100, message = "El proveedor no puede exceder 100 caracteres")
        private String proveedor;
        
        @Size(max = 50, message = "El número de factura no puede exceder 50 caracteres")
        private String numeroFactura;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 100, message = "El concepto no puede exceder 100 caracteres")
        private String concepto;
        
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;
        
        @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a 0")
        private BigDecimal costoUnitario;
        
        private TipoCosto tipo;
        
        @Size(max = 500, message = "La nota no puede exceder 500 caracteres")
        private String nota;
        
        @Size(max = 100, message = "El proveedor no puede exceder 100 caracteres")
        private String proveedor;
        
        @Size(max = 50, message = "El número de factura no puede exceder 50 caracteres")
        private String numeroFactura;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> costos;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
        private BigDecimal totalCostos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenTipo {
        private TipoCosto tipo;
        private String tipoDescripcion;
        private Long cantidad;
        private BigDecimal total;
        private BigDecimal porcentaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        private BigDecimal totalGeneral;
        private List<ResumenTipo> porTipo;
        private LocalDateTime desde;
        private LocalDateTime hasta;
        private Long totalRegistros;
    }
}
