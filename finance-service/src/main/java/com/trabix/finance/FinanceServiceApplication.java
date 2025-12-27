package com.trabix.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de finanzas TRABIX.
 * Puerto: 8085
 * 
 * Funcionalidades:
 * - Configuración de costos (real vs percibido)
 * - Fondo de recompensas (se alimenta cuando VENDEDORES pagan lotes)
 * - Registro de costos de producción
 * 
 * IMPORTANTE:
 * - El fondo solo se alimenta con pagos de VENDEDORES, nunca del ADMIN/dueño
 * - $200 por TRABIX van al fondo (configurable)
 * - Todo se gestiona manualmente por el ADMIN
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.finance", "com.trabix.common"})
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}
