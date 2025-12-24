package com.trabix.billing.repository;

import com.trabix.billing.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Ventas aprobadas de una tanda (para calcular recaudado) */
    List<Venta> findByTandaIdAndEstado(Long tandaId, String estado);
    
    /** Suma el total recaudado de una tanda (ventas aprobadas, excluyendo regalos) */
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = 'APROBADA' 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorTanda(@Param("tandaId") Long tandaId);
    
    /** Suma el total recaudado de un lote (todas las tandas) */
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        JOIN v.tanda t 
        WHERE t.lote.id = :loteId 
        AND v.estado = 'APROBADA' 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorLote(@Param("loteId") Long loteId);
    
    /** Suma recaudado de un usuario en un lote específico */
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        JOIN v.tanda t 
        WHERE t.lote.id = :loteId 
        AND v.usuario.id = :usuarioId 
        AND v.estado = 'APROBADA' 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorUsuarioYLote(
            @Param("usuarioId") Long usuarioId, 
            @Param("loteId") Long loteId);
    
    /** Cuenta unidades vendidas aprobadas de una tanda */
    @Query("""
        SELECT COALESCE(SUM(v.cantidad), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = 'APROBADA'
        """)
    int contarUnidadesVendidasPorTanda(@Param("tandaId") Long tandaId);
}
