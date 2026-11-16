package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    List<Usuario> findByActivoTrueOrderByNombreCompletoAsc();
}
