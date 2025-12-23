package com.trabix.common.enums;

/** Estado de una tanda dentro de un lote */
public enum EstadoTanda {
    PENDIENTE,   // Aún no liberada
    LIBERADA,    // Stock entregado al vendedor
    EN_CUADRE,   // En proceso de cuadre
    CUADRADA     // Cuadre exitoso completado
}
