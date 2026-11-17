package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.service.ParametroService;
import com.cajapiura.preevaluacion.service.SimuladorCreditoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Simulador público de cuotas de crédito (sistema de cuota fija), con
 * tabla de amortización completa. Complementa la pre-evaluación dándole
 * al visitante una herramienta funcional adicional para explorar
 * escenarios antes de solicitar su crédito.
 */
@Controller
@RequestMapping("/simulador")
public class SimuladorController {

    private final SimuladorCreditoService simuladorService;

    private final ParametroService parametros;

    public SimuladorController(SimuladorCreditoService simuladorService, ParametroService parametros) {
        this.simuladorService = simuladorService;
        this.parametros = parametros;
    }

    @GetMapping
    public String simular(
            @RequestParam(required = false, defaultValue = "8000") BigDecimal monto,
            @RequestParam(required = false, defaultValue = "18") Integer plazoMeses,
            @RequestParam(required = false) BigDecimal tasaAnual,
            Model model) {

        // Límites configurables por el administrador (Parámetros del sistema).
        BigDecimal montoMin = parametros.montoMin();
        BigDecimal montoMax = parametros.montoMax();
        int plazoMin = parametros.plazoMin();
        int plazoMax = parametros.plazoMax();
        if (tasaAnual == null) tasaAnual = parametros.tasaDefecto();
        if (monto.compareTo(montoMin) < 0) monto = montoMin;
        if (monto.compareTo(montoMax) > 0) monto = montoMax;
        if (plazoMeses < plazoMin) plazoMeses = plazoMin;
        if (plazoMeses > plazoMax) plazoMeses = plazoMax;
        if (tasaAnual.compareTo(BigDecimal.ZERO) < 0) tasaAnual = BigDecimal.ZERO;
        if (tasaAnual.compareTo(BigDecimal.valueOf(100)) > 0) tasaAnual = BigDecimal.valueOf(100);

        SimuladorCreditoService.ResultadoSimulacion resultado =
                simuladorService.simular(monto, plazoMeses, tasaAnual);

        model.addAttribute("resultado", resultado);
        model.addAttribute("montoInput", monto);
        model.addAttribute("plazoInput", plazoMeses);
        model.addAttribute("tasaInput", tasaAnual);
        model.addAttribute("montoMin", montoMin);
        model.addAttribute("montoMax", montoMax);
        model.addAttribute("plazoMin", plazoMin);
        model.addAttribute("plazoMax", plazoMax);
        return "simulador";
    }
}
