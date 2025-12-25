package com.trabix.notification.repository;

import com.trabix.notification.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCedula(String cedula);
    
    Optional<Usuario> findByCedulaAndEstado(String cedula, String estado);
    
    List<Usuario> findByEstado(String estado);
}
