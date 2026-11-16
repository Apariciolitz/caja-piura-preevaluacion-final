package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.HistorialEstado;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findBySolicitudOrderByFechaAsc(SolicitudCredito solicitud);
}
