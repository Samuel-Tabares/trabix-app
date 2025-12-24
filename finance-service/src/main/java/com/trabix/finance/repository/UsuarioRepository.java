package com.trabix.finance.repository;

import com.trabix.finance.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCedula(String cedula);
    
    Optional<Usuario> findByCedulaAndEstado(String cedula, String estado);
}
