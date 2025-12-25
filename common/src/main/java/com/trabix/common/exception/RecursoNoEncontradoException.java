package com.trabix.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 */
public class RecursoNoEncontradoException extends TrabixException {
    
    public RecursoNoEncontradoException(String recurso, Long id) {
        super(
            String.format("%s con ID %d no encontrado", recurso, id),
            "RECURSO_NO_ENCONTRADO",
            HttpStatus.NOT_FOUND
        );
    }
    public RecursoNoEncontradoException(String mensaje) {
        super(
                String.format("%s no encontrado"),
                "RECURSO_NO_ENCONTRADO",
                HttpStatus.NOT_FOUND
        );
    }


    public RecursoNoEncontradoException(String recurso, String identificador) {
        super(
            String.format("%s '%s' no encontrado", recurso, identificador),
            "RECURSO_NO_ENCONTRADO",
            HttpStatus.NOT_FOUND
        );
    }
}
