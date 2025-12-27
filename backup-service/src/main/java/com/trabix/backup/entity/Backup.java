package com.trabix.backup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Registro de backup en la base de datos.
 * 
 * Guarda metadata del backup, no los datos en sí.
 * Los datos están en el archivo .zip referenciado.
 */
@Entity
@Table(name = "backups", indexes = {
    @Index(name = "idx_backup_estado", columnList = "estado"),
    @Index(name = "idx_backup_fecha", columnList = "fecha_inicio DESC"),
    @Index(name = "idx_backup_created_by", columnList = "created_by")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Ruta completa del archivo .zip
     */
    @Column(nullable = false, length = 500)
    private String archivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoBackup estado = EstadoBackup.EN_PROCESO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // === Estadísticas del backup ===

    @Column(name = "total_usuarios")
    @Builder.Default
    private Integer totalUsuarios = 0;

    @Column(name = "total_ventas")
    @Builder.Default
    private Integer totalVentas = 0;

    @Column(name = "total_lotes")
    @Builder.Default
    private Integer totalLotes = 0;

    @Column(name = "total_tandas")
    @Builder.Default
    private Integer totalTandas = 0;

    @Column(name = "total_asignaciones")
    @Builder.Default
    private Integer totalAsignaciones = 0;

    @Column(name = "total_documentos")
    @Builder.Default
    private Integer totalDocumentos = 0;

    @Column(name = "total_notificaciones")
    @Builder.Default
    private Integer totalNotificaciones = 0;

    @Column(columnDefinition = "TEXT")
    private String notas;

    /**
     * Mensaje de error (si aplica).
     */
    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estado == null) {
            estado = EstadoBackup.EN_PROCESO;
        }
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
    }

    // === Métodos de consulta ===

    public boolean estaEnProceso() {
        return EstadoBackup.EN_PROCESO.equals(estado);
    }

    public boolean estaCompletado() {
        return EstadoBackup.COMPLETADO.equals(estado);
    }

    public boolean tieneError() {
        return EstadoBackup.ERROR.equals(estado);
    }

    // === Métodos de acción ===

    public void completar(long tamano) {
        this.estado = EstadoBackup.COMPLETADO;
        this.fechaFin = LocalDateTime.now();
        this.tamanoBytes = tamano;
    }

    public void marcarError(String mensaje) {
        this.estado = EstadoBackup.ERROR;
        this.fechaFin = LocalDateTime.now();
        this.mensajeError = mensaje;
    }

    // === Métodos de formato ===

    /**
     * Retorna el tamaño en formato legible (KB, MB, GB).
     */
    public String getTamanoFormateado() {
        if (tamanoBytes == null || tamanoBytes == 0) {
            return "0 B";
        }
        
        String[] unidades = {"B", "KB", "MB", "GB"};
        int indice = 0;
        double tamano = tamanoBytes;
        
        while (tamano >= 1024 && indice < unidades.length - 1) {
            tamano /= 1024;
            indice++;
        }
        
        return String.format("%.2f %s", tamano, unidades[indice]);
    }

    /**
     * Retorna la duración del backup en segundos.
     */
    public Long getDuracionSegundos() {
        if (fechaInicio == null || fechaFin == null) {
            return null;
        }
        return Duration.between(fechaInicio, fechaFin).getSeconds();
    }

    /**
     * Retorna la duración formateada.
     */
    public String getDuracionFormateada() {
        Long segundos = getDuracionSegundos();
        if (segundos == null) {
            return "-";
        }
        if (segundos < 60) {
            return segundos + " seg";
        }
        long minutos = segundos / 60;
        long segs = segundos % 60;
        return minutos + " min " + segs + " seg";
    }
}
