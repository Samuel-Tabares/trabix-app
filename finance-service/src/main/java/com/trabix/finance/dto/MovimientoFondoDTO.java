package com.trabix.finance.dto;

import com.trabix.finance.entity.ReferenciaMovimiento;
import com.trabix.finance.entity.TipoMovimientoFondo;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        private TipoMovimientoFondo tipo;
        private String tipoDescripcion;
        private BigDecimal monto;
        private LocalDateTime fecha;
        private String descripcion;
        private UsuarioInfo beneficiario;
        private UsuarioInfo vendedorOrigen;
        private BigDecimal saldoPosterior;
        private Long referenciaId;
        private ReferenciaMovimiento referenciaTipo;
        private String referenciaTipoDescripcion;
        private Integer cantidadTrabix;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioInfo {
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
        private List<Response> movimientos;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenVendedor {
        private Long vendedorId;
        private String nombre;
        private String cedula;
        private BigDecimal totalAportado;
        private Long totalTrabix;
        private Long cantidadPagos;
    }
}
