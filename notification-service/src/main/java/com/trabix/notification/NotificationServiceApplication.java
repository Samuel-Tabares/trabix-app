package com.trabix.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de notificaciones TRABIX.
 * Puerto: 8088
 * 
 * Funcionalidades:
 * - Notificaciones internas del sistema
 * - Alertas y recordatorios
 * - Broadcast a todos los usuarios
 * - Marcado de lectura
 * - Limpieza automática de antiguas
 * 
 * Tipos de notificación:
 * - INFO: Información general
 * - ALERTA: Alertas importantes
 * - RECORDATORIO: Recordatorios
 * - SISTEMA: Mensajes del sistema
 * - EXITO: Operaciones exitosas
 * - ERROR: Errores o problemas
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.notification", "com.trabix.common"})
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
