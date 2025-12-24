package com.trabix.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs para Movimientos del Fondo.
 */
public class MovimientoFondoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String tipo;
        private BigDecimal monto;
        private LocalDateTime fecha;
        private String descripcion;
        private BeneficiarioInfo beneficiario;
        private BigDecimal saldoPosterior;
        private Long referenciaId;
        private String referenciaTipo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiarioInfo {
        private Long id;
        private String cedula;
        private String nombre;
        private String nivel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private java.util.List<Response> movimientos;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenBeneficiario {
        private Long beneficiarioId;
        private String nombre;
        private String cedula;
        private BigDecimal totalPremios;
        private Long cantidadPremios;
    }
}
