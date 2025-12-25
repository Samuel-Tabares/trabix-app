package com.trabix.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Notificaciones.
 */
public class NotificacionDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long usuarioId;
        private String usuarioNombre;
        private String tipo;
        private String titulo;
        private String mensaje;
        private Boolean leida;
        private LocalDateTime fechaLectura;
        private String referenciaTipo;
        private Long referenciaId;
        private LocalDateTime createdAt;
        private boolean esBroadcast;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        private Long usuarioId;
        
        @Pattern(regexp = "INFO|ALERTA|RECORDATORIO|SISTEMA|EXITO|ERROR", 
                 message = "Tipo inválido. Use: INFO, ALERTA, RECORDATORIO, SISTEMA, EXITO o ERROR")
        private String tipo;
        
        @NotBlank(message = "El título es requerido")
        @Size(max = 100, message = "El título no puede exceder 100 caracteres")
        private String titulo;
        
        @NotBlank(message = "El mensaje es requerido")
        @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
        private String mensaje;
        
        private String referenciaTipo;
        
        private Long referenciaId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastRequest {
        
        @Pattern(regexp = "INFO|ALERTA|RECORDATORIO|SISTEMA|EXITO|ERROR", 
                 message = "Tipo inválido")
        private String tipo;
        
        @NotBlank(message = "El título es requerido")
        @Size(max = 100, message = "El título no puede exceder 100 caracteres")
        private String titulo;
        
        @NotBlank(message = "El mensaje es requerido")
        @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
        private String mensaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarcarLeidasRequest {
        @NotEmpty(message = "Debe incluir al menos un ID")
        private List<Long> ids;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> notificaciones;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
        private long noLeidas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContadorResponse {
        private long total;
        private long noLeidas;
        private long leidas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenTipos {
        private long info;
        private long alerta;
        private long recordatorio;
        private long sistema;
        private long exito;
        private long error;
    }
}
