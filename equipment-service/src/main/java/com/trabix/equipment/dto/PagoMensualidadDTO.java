package com.trabix.equipment.dto;

import lombok.*;

import java.math.BigDecimal;
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
        private Long equipoId;
        private String tipoEquipo;
        private Long usuarioId;
        private String usuarioNombre;
        private String usuarioCedula;
        private Integer mes;
        private Integer anio;
        private String periodo;
        private BigDecimal monto;
        private LocalDateTime fechaPago;
        private String estado;
        private String nota;
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
        private String nota;
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
        private BigDecimal montoPagado;
        private BigDecimal montoPendiente;
    }
}
