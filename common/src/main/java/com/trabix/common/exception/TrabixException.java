package com.trabix.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción base para todas las excepciones del sistema TRABIX.
 * Incluye código de error y status HTTP para respuestas consistentes.
 */
@Getter
public class TrabixException extends RuntimeException {
    
    private final String codigo;
    private final HttpStatus status;

    public TrabixException(String mensaje, String codigo, HttpStatus status) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }

    public TrabixException(String mensaje, String codigo, HttpStatus status, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
        this.status = status;
    }
}
