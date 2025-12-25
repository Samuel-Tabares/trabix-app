package com.trabix.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de documentos TRABIX.
 * Puerto: 8087
 * 
 * Funcionalidades:
 * - Cotizaciones para clientes
 * - Facturas de venta
 * - Conversión cotización → factura
 * - Numeración automática
 */
@SpringBootApplication(scanBasePackages = {"com.trabix.document", "com.trabix.common"})
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
