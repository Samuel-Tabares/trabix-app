package com.trabix.billing.repository;

import com.trabix.billing.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCedula(String cedula);
    
    Optional<Usuario> findByCedulaAndEstado(String cedula, String estado);
    
    /** Obtiene la cadena de reclutadores hacia arriba (para distribución cascada) */
    @Query(value = """
        WITH RECURSIVE cadena AS (
            SELECT id, nombre, cedula, nivel, reclutador_id, 1 as profundidad
            FROM usuarios WHERE id = :usuarioId
            UNION ALL
            SELECT u.id, u.nombre, u.cedula, u.nivel, u.reclutador_id, c.profundidad + 1
            FROM usuarios u
            INNER JOIN cadena c ON u.id = c.reclutador_id
            WHERE c.profundidad < 10
        )
        SELECT * FROM cadena ORDER BY profundidad ASC
        """, nativeQuery = true)
    List<Object[]> obtenerCadenaReclutadores(@Param("usuarioId") Long usuarioId);
    
    /** Obtiene el admin (N1) */
    @Query("SELECT u FROM Usuario u WHERE u.nivel = 'N1' OR u.rol = 'ADMIN'")
    Optional<Usuario> findAdmin();
}
