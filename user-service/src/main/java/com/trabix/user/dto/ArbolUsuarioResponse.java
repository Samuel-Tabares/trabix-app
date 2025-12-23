package com.trabix.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta con el árbol de cascada de un usuario.
 * Muestra la jerarquía de reclutados recursivamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArbolUsuarioResponse {

    private Long id;
    private String nombre;
    private String cedula;
    private String nivel;
    private String modeloNegocio;
    private String estado;
    
    @Builder.Default
    private List<ArbolUsuarioResponse> reclutados = new ArrayList<>();
    
    // Estadísticas del subárbol
    private Integer totalDirectos;      // Reclutados directos
    private Integer totalIndirectos;    // Reclutados de reclutados (toda la rama)
}
