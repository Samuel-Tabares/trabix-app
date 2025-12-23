package com.trabix.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción para accesos no autorizados (rol insuficiente).
 */
public class AccesoNoAutorizadoException extends TrabixException {
    
    public AccesoNoAutorizadoException() {
        super("No tiene permisos para realizar esta acción", "ACCESO_NO_AUTORIZADO", HttpStatus.FORBIDDEN);
    }

    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje, "ACCESO_NO_AUTORIZADO", HttpStatus.FORBIDDEN);
    }
}
