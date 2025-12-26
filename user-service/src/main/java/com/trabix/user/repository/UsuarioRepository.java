package com.trabix.user.repository;

import com.trabix.common.enums.EstadoUsuario;
import com.trabix.common.enums.RolUsuario;
import com.trabix.user.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCedula(String cedula);

    Optional<Usuario> findByCedulaAndEstado(String cedula, EstadoUsuario estado);

    boolean existsByCedula(String cedula);

    boolean existsByCorreo(String correo);

    // Buscar por estado
    Page<Usuario> findByEstado(EstadoUsuario estado, Pageable pageable);

    // Buscar por rol
    List<Usuario> findByRolAndEstado(RolUsuario rol, EstadoUsuario estado);

    // Buscar reclutados directos de un usuario
    List<Usuario> findByReclutadorIdAndEstado(Long reclutadorId, EstadoUsuario estado);

    // Contar reclutados directos
    long countByReclutadorIdAndEstado(Long reclutadorId, EstadoUsuario estado);

    // Buscar usuarios por nivel
    List<Usuario> findByNivelAndEstado(String nivel, EstadoUsuario estado);

    // Buscar vendedores activos (excluyendo admin)
    @Query("SELECT u FROM Usuario u WHERE u.rol != 'ADMIN' AND u.estado = :estado")
    Page<Usuario> findVendedoresActivos(@Param("estado") EstadoUsuario estado, Pageable pageable);

    // Buscar por nombre (para búsqueda)
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) AND u.estado = :estado")
    List<Usuario> buscarPorNombre(@Param("nombre") String nombre, @Param("estado") EstadoUsuario estado);

    // Obtener toddo el árbol hacia abajo desde un usuario (reclutados recursivos)
    @Query(value = """
        WITH RECURSIVE arbol AS (
            SELECT id, nombre, cedula, nivel, estado, reclutador_id, 1 as profundidad
            FROM usuarios WHERE id = :usuarioId
            UNION ALL
            SELECT u.id, u.nombre, u.cedula, u.nivel, u.estado, u.reclutador_id, a.profundidad + 1
            FROM usuarios u
            INNER JOIN arbol a ON u.reclutador_id = a.id
            WHERE a.profundidad < 10
        )
        SELECT * FROM arbol WHERE id != :usuarioId
        """, nativeQuery = true)
    List<Object[]> obtenerArbolReclutados(@Param("usuarioId") Long usuarioId);

    // Contar total de reclutados en toddo el árbol
    @Query(value = """
        WITH RECURSIVE arbol AS (
            SELECT id FROM usuarios WHERE id = :usuarioId
            UNION ALL
            SELECT u.id FROM usuarios u
            INNER JOIN arbol a ON u.reclutador_id = a.id
        )
        SELECT COUNT(*) - 1 FROM arbol
        """, nativeQuery = true)
    long contarTotalReclutados(@Param("usuarioId") Long usuarioId);

    /**
     * Verifica si un usuario está en la cadena de reclutados de otro (hacia abajo).
     * Retorna 1 si usuarioId está en el árbol de reclutadorId, 0 si no.
     */
    @Query(value = """
        WITH RECURSIVE arbol AS (
            SELECT id FROM usuarios WHERE reclutador_id = :reclutadorId
            UNION ALL
            SELECT u.id FROM usuarios u
            INNER JOIN arbol a ON u.reclutador_id = a.id
        )
        SELECT COUNT(*) FROM arbol WHERE id = :usuarioId
        """, nativeQuery = true)
    long verificarEsReclutadoDe(@Param("usuarioId") Long usuarioId, @Param("reclutadorId") Long reclutadorId);

    /**
     * Obtiene el nivel más profundo en toddo el sistema.
     */
    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(nivel FROM 2) AS INTEGER)) FROM usuarios WHERE estado = 'ACTIVO'
        """, nativeQuery = true)
    Integer obtenerNivelMaximo();
}
