package com.trabix.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción para credenciales inválidas (login fallido).
 */
public class CredencialesInvalidasException extends TrabixException {
    
    public CredencialesInvalidasException() {
        super("Credenciales inválidas", "CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED);
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje, "CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED);
    }
}
