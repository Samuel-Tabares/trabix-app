package com.trabix.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción para errores de validación de reglas de negocio.
 */
public class ValidacionNegocioException extends TrabixException {
    
    public ValidacionNegocioException(String mensaje) {
        super(mensaje, "VALIDACION_NEGOCIO", HttpStatus.BAD_REQUEST);
    }

    public ValidacionNegocioException(String mensaje, String codigo) {
        super(mensaje, codigo, HttpStatus.BAD_REQUEST);
    }
}
