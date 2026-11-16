package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.EstadoSolicitud;
import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCredito, Long> {

    List<SolicitudCredito> findAllByOrderByFechaCreacionDesc();

    List<SolicitudCredito> findByResultadoOrderByFechaCreacionDesc(ResultadoEvaluacion resultado);

    List<SolicitudCredito> findByEstadoOrderByFechaCreacionDesc(EstadoSolicitud estado);

    long countByResultado(ResultadoEvaluacion resultado);

    long countByEstado(EstadoSolicitud estado);
}
