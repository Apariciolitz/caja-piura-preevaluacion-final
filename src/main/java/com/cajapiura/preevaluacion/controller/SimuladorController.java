package com.cajapiura.preevaluacion.controller;

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

    public SimuladorController(SimuladorCreditoService simuladorService) {
        this.simuladorService = simuladorService;
    }

    @GetMapping
    public String simular(
            @RequestParam(required = false, defaultValue = "8000") BigDecimal monto,
            @RequestParam(required = false, defaultValue = "18") Integer plazoMeses,
            @RequestParam(required = false, defaultValue = "24") BigDecimal tasaAnual,
            Model model) {

        // Límites razonables para evitar cálculos absurdos desde la URL.
        if (monto.compareTo(BigDecimal.valueOf(500)) < 0) monto = BigDecimal.valueOf(500);
        if (monto.compareTo(BigDecimal.valueOf(200000)) > 0) monto = BigDecimal.valueOf(200000);
        if (plazoMeses < 3) plazoMeses = 3;
        if (plazoMeses > 60) plazoMeses = 60;
        if (tasaAnual.compareTo(BigDecimal.ZERO) < 0) tasaAnual = BigDecimal.ZERO;
        if (tasaAnual.compareTo(BigDecimal.valueOf(100)) > 0) tasaAnual = BigDecimal.valueOf(100);

        SimuladorCreditoService.ResultadoSimulacion resultado =
                simuladorService.simular(monto, plazoMeses, tasaAnual);

        model.addAttribute("resultado", resultado);
        model.addAttribute("montoInput", monto);
        model.addAttribute("plazoInput", plazoMeses);
        model.addAttribute("tasaInput", tasaAnual);
        return "simulador";
    }
}
