package com.trabix.equipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de equipos TRABIX.
 * Puerto: 8086
 * 
 * MODELO DE NEGOCIO:
 * - Kit = Nevera + Pijama (siempre van juntos)
 * - Solo 1 kit por vendedor
 * - Mensualidad: $10,000/mes por el kit
 * - Se paga primero, luego se asigna
 * - Día de pago = mismo día que pagó primera vez
 * 
 * STOCK:
 * - Admin tiene X kits disponibles
 * - Al asignar: disponibles - 1
 * - Al devolver o reponer: disponibles + 1
 * 
 * PÉRDIDA/DAÑO:
 * - Nevera: $25,000 | Pijama: $55,000 (configurable)
 * - Se cancela mensualidad hasta que pague y se reponga
 * 
 * PAGOS PENDIENTES:
 * - Bloquean el flujo de cuadres (no puede desbloquear tandas)
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.equipment", "com.trabix.common"})
@EnableScheduling
public class EquipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentServiceApplication.class, args);
    }
}
