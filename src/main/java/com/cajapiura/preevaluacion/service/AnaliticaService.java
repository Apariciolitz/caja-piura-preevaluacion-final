package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.repository.SolicitudCreditoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula las métricas que alimentan el dashboard de analítica del asesor:
 * tendencia de solicitudes por día, distribución por destino de crédito,
 * y tasa de pre-calificación vs. revisión.
 */
@Service
public class AnaliticaService {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final SolicitudCreditoRepository repository;

    public AnaliticaService(SolicitudCreditoRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> construirMetricas() {
        List<SolicitudCredito> todas = repository.findAll();

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("tendenciaDias", tendenciaUltimosDias(todas, 14));
        metricas.put("distribucionDestino", distribucionPorDestino(todas));
        metricas.put("conteoResultado", conteoPorResultado(todas));
        metricas.put("montoPromedioSolicitado", montoPromedio(todas));
        metricas.put("tasaPrecalificacion", tasaPrecalificacion(todas));
        return metricas;
    }

    private Map<String, Long> tendenciaUltimosDias(List<SolicitudCredito> todas, int dias) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            long cantidad = todas.stream()
                    .filter(s -> s.getFechaCreacion() != null && s.getFechaCreacion().toLocalDate().isEqual(dia))
                    .count();
            resultado.put(dia.format(FORMATO_DIA), cantidad);
        }
        return resultado;
    }

    private Map<String, Long> distribucionPorDestino(List<SolicitudCredito> todas) {
        return todas.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getDestinoCredito() == null ? "Sin especificar" : s.getDestinoCredito(),
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private Map<String, Long> conteoPorResultado(List<SolicitudCredito> todas) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        conteo.put("Pre-calificado", todas.stream().filter(s -> s.getResultado() == ResultadoEvaluacion.PRECALIFICADO).count());
        conteo.put("Requiere revisión", todas.stream().filter(s -> s.getResultado() == ResultadoEvaluacion.REQUIERE_REVISION).count());
        return conteo;
    }

    private double montoPromedio(List<SolicitudCredito> todas) {
        return todas.stream()
                .filter(s -> s.getMontoSolicitado() != null)
                .mapToDouble(s -> s.getMontoSolicitado().doubleValue())
                .average()
                .orElse(0.0);
    }

    private double tasaPrecalificacion(List<SolicitudCredito> todas) {
        if (todas.isEmpty()) {
            return 0.0;
        }
        long precalificados = todas.stream().filter(s -> s.getResultado() == ResultadoEvaluacion.PRECALIFICADO).count();
        return (precalificados * 100.0) / todas.size();
    }
}
