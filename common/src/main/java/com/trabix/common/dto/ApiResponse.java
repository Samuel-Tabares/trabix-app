package com.trabix.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta estándar para todas las operaciones de la API.
 * Envuelve la respuesta con metadatos y manejo de errores consistente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean exito;
    private String mensaje;
    private T datos;
    private ErrorInfo error;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Respuesta exitosa con datos */
    public static <T> ApiResponse<T> ok(T datos) {
        return ApiResponse.<T>builder()
                .exito(true)
                .datos(datos)
                .build();
    }

    /** Respuesta exitosa con datos y mensaje */
    public static <T> ApiResponse<T> ok(T datos, String mensaje) {
        return ApiResponse.<T>builder()
                .exito(true)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }

    /** Respuesta exitosa solo con mensaje */
    public static <T> ApiResponse<T> ok(String mensaje) {
        return ApiResponse.<T>builder()
                .exito(true)
                .mensaje(mensaje)
                .build();
    }

    /** Respuesta de error */
    public static <T> ApiResponse<T> error(String codigo, String mensaje) {
        return ApiResponse.<T>builder()
                .exito(false)
                .error(ErrorInfo.builder()
                        .codigo(codigo)
                        .mensaje(mensaje)
                        .build())
                .build();
    }

    /** Respuesta de error con detalles */
    public static <T> ApiResponse<T> error(String codigo, String mensaje, Object detalles) {
        return ApiResponse.<T>builder()
                .exito(false)
                .error(ErrorInfo.builder()
                        .codigo(codigo)
                        .mensaje(mensaje)
                        .detalles(detalles)
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        private String codigo;
        private String mensaje;
        private Object detalles;
    }
}
