package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /** Los 500 eventos más recientes; el filtrado fino se hace en AuditoriaService. */
    List<RegistroAuditoria> findTop500ByOrderByFechaDesc();
}
