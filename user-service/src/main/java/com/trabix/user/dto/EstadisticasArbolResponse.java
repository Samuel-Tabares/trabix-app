package com.trabix.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta con estadísticas generales del árbol de usuarios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstadisticasArbolResponse {

    /**
     * Nivel más profundo actual en el sistema (ej: 5 si hay usuarios N5).
     */
    private Integer nivelMaximoActual;

    /**
     * Límite máximo de niveles permitido.
     */
    private Integer nivelLimite;

    /**
     * Nivel a partir del cual se genera alerta inicial.
     */
    private Integer nivelAlertaInicial;

    /**
     * Nivel a partir del cual se genera alerta crítica.
     */
    private Integer nivelAlertaCritico;

    /**
     * Total de usuarios registrados.
     */
    private Integer totalUsuarios;

    /**
     * Total de usuarios activos.
     */
    private Integer usuariosActivos;

    /**
     * Mensaje de alerta si aplica (null si no hay alerta).
     */
    private String alertaNivel;
}
