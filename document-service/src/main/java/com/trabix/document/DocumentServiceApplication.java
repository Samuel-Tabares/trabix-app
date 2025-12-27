package com.trabix.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de documentos TRABIX.
 * Puerto: 8087
 * 
 * Funcionalidades:
 * - Cotizaciones para clientes (solo ADMIN)
 * - Facturas de venta (después del pago)
 * - Conversión cotización → factura
 * - Numeración automática por año
 * - Marcado automático de cotizaciones vencidas
 * 
 * IMPORTANTE:
 * - Solo el ADMIN maneja documentos
 * - Items son solo TRABIX (granizados)
 * - NO se puede anular documento PAGADO
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.document", "com.trabix.common"})
@EnableScheduling
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
