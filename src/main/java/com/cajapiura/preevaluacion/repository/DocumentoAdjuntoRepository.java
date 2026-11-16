package com.cajapiura.preevaluacion.repository;

import com.cajapiura.preevaluacion.model.DocumentoAdjunto;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {

    List<DocumentoAdjunto> findBySolicitudOrderByFechaCargaAsc(SolicitudCredito solicitud);
}
