package com.trabix.document.dto;

import com.trabix.document.entity.EstadoDocumento;
import com.trabix.document.entity.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Documentos (Cotizaciones y Facturas).
 */
public class DocumentoDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private TipoDocumento tipo;
        private String tipoDescripcion;
        private String numero;
        private Long usuarioId;
        private String usuarioNombre;
        private String clienteNombre;
        private String clienteTelefono;
        private String clienteDireccion;
        private String clienteNit;
        private String clienteCorreo;
        private List<ItemDocumento> items;
        private BigDecimal subtotal;
        private BigDecimal iva;
        private BigDecimal total;
        private LocalDateTime fechaEmision;
        private LocalDateTime fechaVencimiento;
        private EstadoDocumento estado;
        private String estadoDescripcion;
        private String notas;
        private Long cotizacionOrigenId;
        private String cotizacionOrigenNumero;
        private LocalDateTime createdAt;
        
        // Flags de acciones permitidas
        private boolean puedeEditarse;
        private boolean puedeEmitirse;
        private boolean puedePagarse;
        private boolean puedeAnularse;
        private boolean puedeConvertirse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "El tipo es requerido")
        private TipoDocumento tipo;
        
        @NotBlank(message = "El nombre del cliente es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String clienteNombre;
        
        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        private String clienteTelefono;
        
        @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
        private String clienteDireccion;
        
        @Size(max = 20, message = "El NIT no puede exceder 20 caracteres")
        private String clienteNit;
        
        @Email(message = "Correo inválido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        private String clienteCorreo;
        
        @NotEmpty(message = "Debe incluir al menos un item")
        @Valid
        private List<ItemDocumento> items;
        
        private Boolean aplicarIva;
        
        @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
        private String notas;
        
        /**
         * Días de vencimiento para cotizaciones (default: 15).
         */
        @Min(value = 1, message = "Los días de vencimiento deben ser al menos 1")
        private Integer diasVencimiento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String clienteNombre;
        
        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        private String clienteTelefono;
        
        @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
        private String clienteDireccion;
        
        @Size(max = 20, message = "El NIT no puede exceder 20 caracteres")
        private String clienteNit;
        
        @Email(message = "Correo inválido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        private String clienteCorreo;
        
        @Valid
        private List<ItemDocumento> items;
        
        private Boolean aplicarIva;
        
        @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
        private String notas;
    }

    /**
     * Item del documento.
     * Solo TRABIX (granizados) por ahora.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDocumento {
        
        @NotBlank(message = "La descripción es requerida")
        @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
        private String descripcion;
        
        @NotNull(message = "La cantidad es requerida")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;
        
        @NotNull(message = "El precio unitario es requerido")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        private BigDecimal precioUnitario;
        
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> documentos;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConvertirAFacturaRequest {
        @Size(max = 20, message = "El NIT no puede exceder 20 caracteres")
        private String clienteNit;
        
        @Email(message = "Correo inválido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        private String clienteCorreo;
        
        @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
        private String notas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenDocumentos {
        private TipoDocumento tipo;
        private String tipoDescripcion;
        private long total;
        private long borradores;
        private long emitidos;
        private long pagados;
        private long anulados;
        private long vencidos;
        private BigDecimal totalPagado;
        private BigDecimal totalPendiente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenPeriodo {
        private LocalDateTime desde;
        private LocalDateTime hasta;
        private long cotizaciones;
        private long facturas;
        private BigDecimal totalCotizaciones;
        private BigDecimal totalFacturas;
        private BigDecimal totalRecaudado;
    }
}
