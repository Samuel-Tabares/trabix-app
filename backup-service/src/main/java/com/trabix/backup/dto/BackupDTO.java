package com.trabix.backup.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para Backups.
 */
public class BackupDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nombre;
        private String archivo;
        private Long tamanoBytes;
        private String tamanoFormateado;
        private String estado;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private Long duracionSegundos;
        private Integer totalUsuarios;
        private Integer totalVentas;
        private Integer totalLotes;
        private Integer totalTandas;
        private Integer totalEquipos;
        private Integer totalDocumentos;
        private Integer totalNotificaciones;
        private String notas;
        private Long createdBy;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String notas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> backups;
        private int pagina;
        private int tamano;
        private long totalElementos;
        private int totalPaginas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenResponse {
        private long totalBackups;
        private long backupsCompletados;
        private long backupsConError;
        private String tamanoTotalFormateado;
        private Long tamanoTotalBytes;
        private Response ultimoBackup;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasBackup {
        private int totalUsuarios;
        private int totalVentas;
        private int totalLotes;
        private int totalTandas;
        private int totalEquipos;
        private int totalDocumentos;
        private int totalNotificaciones;
    }
}
