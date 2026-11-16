package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.service.AnaliticaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/** Dashboard de analítica para el panel del asesor, con gráficos (Chart.js). */
@Controller
@RequestMapping("/asesor/analitica")
public class AnaliticaController {

    private final AnaliticaService analiticaService;
    private final ObjectMapper objectMapper;

    public AnaliticaController(AnaliticaService analiticaService, ObjectMapper objectMapper) {
        this.analiticaService = analiticaService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String analitica(Model model) throws Exception {
        Map<String, Object> metricas = analiticaService.construirMetricas();

        model.addAttribute("metricasJson", objectMapper.writeValueAsString(metricas));
        model.addAttribute("tasaPrecalificacion", metricas.get("tasaPrecalificacion"));
        model.addAttribute("montoPromedio", metricas.get("montoPromedioSolicitado"));
        return "asesor/analitica";
    }
}
