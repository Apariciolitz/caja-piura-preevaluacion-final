package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.EstadoSolicitud;
import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCredito, Long> {

    List<SolicitudCredito> findAllByOrderByFechaCreacionDesc();

    List<SolicitudCredito> findByResultadoOrderByFechaCreacionDesc(ResultadoEvaluacion resultado);

    List<SolicitudCredito> findByEstadoOrderByFechaCreacionDesc(EstadoSolicitud estado);

    Optional<SolicitudCredito> findByCodigoPublico(String codigoPublico);

    /** Casos activos de un asesor (INC-07: reemplaza findAll() dentro del comparador). */
    long countByAsesorAsignadoAndEstadoNot(Usuario asesor, EstadoSolicitud estado);

    long countByResultado(ResultadoEvaluacion resultado);

    long countByEstado(EstadoSolicitud estado);
}
