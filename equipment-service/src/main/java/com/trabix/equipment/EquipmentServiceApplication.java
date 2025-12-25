package com.trabix.equipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de equipos TRABIX.
 * Puerto: 8086
 * 
 * Funcionalidades:
 * - Gestión de equipos (neveras, pijamas)
 * - Asignación a vendedores
 * - Control de mensualidades ($10,000/mes)
 * - Registro de pagos
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.equipment", "com.trabix.common"})
@EnableScheduling
public class EquipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentServiceApplication.class, args);
    }
}
