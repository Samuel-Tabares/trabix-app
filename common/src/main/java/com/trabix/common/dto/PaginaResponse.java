package com.trabix.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta paginada para listados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponse<T> {
    
    private List<T> contenido;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
    private int tamanioPagina;
    private boolean primera;
    private boolean ultima;

    public static <T> PaginaResponse<T> of(List<T> contenido, int pagina, int tamanio, long total) {
        int totalPaginas = (int) Math.ceil((double) total / tamanio);
        return PaginaResponse.<T>builder()
                .contenido(contenido)
                .paginaActual(pagina)
                .totalPaginas(totalPaginas)
                .totalElementos(total)
                .tamanioPagina(tamanio)
                .primera(pagina == 0)
                .ultima(pagina >= totalPaginas - 1)
                .build();
    }
}
