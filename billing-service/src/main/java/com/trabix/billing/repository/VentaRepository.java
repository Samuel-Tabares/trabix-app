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

    List<Venta> findByTandaIdAndEstado(Long tandaId, String estado);
    
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = 'APROBADA' 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorTanda(@Param("tandaId") Long tandaId);
    
    @Query("""
        SELECT COALESCE(SUM(v.precioTotal), 0) FROM Venta v 
        JOIN v.tanda t 
        WHERE t.lote.id = :loteId 
        AND v.estado = 'APROBADA' 
        AND v.tipo != 'REGALO'
        """)
    BigDecimal sumarRecaudadoPorLote(@Param("loteId") Long loteId);
    
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
    
    @Query("""
        SELECT COALESCE(SUM(v.cantidad), 0) FROM Venta v 
        WHERE v.tanda.id = :tandaId 
        AND v.estado = 'APROBADA'
        """)
    int contarUnidadesVendidasPorTanda(@Param("tandaId") Long tandaId);
}
