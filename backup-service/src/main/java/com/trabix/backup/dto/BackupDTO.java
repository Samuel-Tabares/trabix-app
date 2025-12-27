package com.trabix.backup.dto;

import com.trabix.backup.entity.EstadoBackup;
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
        private EstadoBackup estado;
        private String estadoDescripcion;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private Long duracionSegundos;
        private String duracionFormateada;
        private Integer totalUsuarios;
        private Integer totalVentas;
        private Integer totalLotes;
        private Integer totalTandas;
        private Integer totalAsignaciones;
        private Integer totalDocumentos;
        private Integer totalNotificaciones;
        private String notas;
        private String mensajeError;
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
        private long backupsEnProceso;
        private long backupsConError;
        private String tamanoTotalFormateado;
        private Long tamanoTotalBytes;
        private Response ultimoBackupCompletado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasActuales {
        private int totalUsuarios;
        private int totalVentas;
        private int totalLotes;
        private int totalTandas;
        private int totalAsignaciones;
        private int stockDisponible;
        private int totalDocumentos;
        private int totalNotificaciones;
        private int totalCostosProduccion;
        private int totalMovimientosFondo;
    }
}
