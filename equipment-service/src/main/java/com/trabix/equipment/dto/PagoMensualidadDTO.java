package com.trabix.equipment.dto;

import com.trabix.equipment.entity.EstadoPago;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Pagos de Mensualidad.
 */
public class PagoMensualidadDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long asignacionId;
        private Long usuarioId;
        private String usuarioNombre;
        private String usuarioCedula;
        private Integer mes;
        private Integer anio;
        private String periodo;
        private BigDecimal monto;
        private LocalDate fechaVencimiento;
        private LocalDateTime fechaPago;
        private EstadoPago estado;
        private String estadoDescripcion;
        private String nota;
        private boolean vencido;
        private int diasVencido;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> pagos;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
        private BigDecimal totalMonto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrarPagoRequest {
        @Size(max = 500, message = "La nota no puede exceder 500 caracteres")
        private String nota;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerarMensualidadesRequest {
        @Min(value = 1, message = "El mes debe ser entre 1 y 12")
        @Max(value = 12, message = "El mes debe ser entre 1 y 12")
        private Integer mes;
        
        @Min(value = 2020, message = "El año debe ser válido")
        @Max(value = 2100, message = "El año debe ser válido")
        private Integer anio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenMes {
        private Integer mes;
        private Integer anio;
        private String periodo;
        private long totalPagos;
        private long pagados;
        private long pendientes;
        private long vencidos;
        private BigDecimal montoPagado;
        private BigDecimal montoPendiente;
        private BigDecimal montoVencido;
    }
}
