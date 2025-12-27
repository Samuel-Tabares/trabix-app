package com.trabix.backup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Servicio de respaldos TRABIX.
 * Puerto: 8089
 * 
 * Funcionalidades:
 * - Backups COMPLETOS de toda la base de datos
 * - Archivos .zip permanentes (nunca se borran automáticamente)
 * - Descarga de backups
 * - Historial de respaldos
 * 
 * IMPORTANTE: Los backups se guardan en la ruta configurada en:
 * trabix.backup.ruta (variable de entorno BACKUP_PATH)
 * 
 * TABLAS INCLUIDAS EN BACKUP:
 * - usuarios
 * - ventas
 * - lotes
 * - tandas
 * - asignaciones_equipo (kits nevera+pijama)
 * - stock_equipos
 * - pagos_mensualidad
 * - documentos
 * - notificaciones
 * - configuracion_costos
 * - costos_produccion
 * - fondo_recompensas
 * - movimientos_fondo
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.backup", "com.trabix.common"})
@EnableAsync
public class BackupServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackupServiceApplication.class, args);
    }
}
