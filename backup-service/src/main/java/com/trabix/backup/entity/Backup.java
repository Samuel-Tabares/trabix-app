package com.trabix.backup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de backup en la base de datos.
 * 
 * Guarda metadata del backup, no los datos en sí.
 * Los datos están en el archivo .zip referenciado.
 */
@Entity
@Table(name = "backups")
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

    @Column(nullable = false, length = 255)
    private String archivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "EN_PROCESO";

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

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

    @Column(name = "total_equipos")
    @Builder.Default
    private Integer totalEquipos = 0;

    @Column(name = "total_documentos")
    @Builder.Default
    private Integer totalDocumentos = 0;

    @Column(name = "total_notificaciones")
    @Builder.Default
    private Integer totalNotificaciones = 0;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (estado == null) {
            estado = "EN_PROCESO";
        }
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
    }

    public boolean estaEnProceso() {
        return "EN_PROCESO".equals(estado);
    }

    public boolean estaCompletado() {
        return "COMPLETADO".equals(estado);
    }

    public boolean tieneError() {
        return "ERROR".equals(estado);
    }

    public void completar(long tamano) {
        this.estado = "COMPLETADO";
        this.fechaFin = LocalDateTime.now();
        this.tamanoBytes = tamano;
    }

    public void marcarError(String mensaje) {
        this.estado = "ERROR";
        this.fechaFin = LocalDateTime.now();
        this.notas = mensaje;
    }

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
        return java.time.Duration.between(fechaInicio, fechaFin).getSeconds();
    }
}
