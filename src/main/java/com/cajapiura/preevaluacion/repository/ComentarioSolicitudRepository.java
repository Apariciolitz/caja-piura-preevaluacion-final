package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.ComentarioSolicitud;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioSolicitudRepository extends JpaRepository<ComentarioSolicitud, Long> {

    List<ComentarioSolicitud> findBySolicitudOrderByFechaAsc(SolicitudCredito solicitud);
}
