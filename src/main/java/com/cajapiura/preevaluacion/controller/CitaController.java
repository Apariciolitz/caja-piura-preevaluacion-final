package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.service.PreEvaluacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/cita")
public class CitaController {

    private final PreEvaluacionService service;

    public CitaController(PreEvaluacionService service) {
        this.service = service;
    }

    @GetMapping("/{codigo}")
    public String formulario(@PathVariable String codigo, Model model) {
        return service.buscarPorCodigo(codigo)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    model.addAttribute("fechaMinima", LocalDate.now().plusDays(1));
                    return "cita";
                })
                .orElse("redirect:/pre-evaluacion");
    }

    @PostMapping("/{codigo}")
    public String agendar(@PathVariable String codigo,
                           @RequestParam String fecha,
                           @RequestParam String hora,
                           @RequestParam String agencia) {
        // INC-05: fecha inválida o pasada ya no produce HTTP 500; se vuelve al formulario con un aviso.
        var solicitud = service.buscarPorCodigo(codigo);
        if (solicitud.isEmpty()) {
            return "redirect:/pre-evaluacion";
        }
        try {
            service.agendarCita(solicitud.get().getId(), LocalDate.parse(fecha), hora, agencia);
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            return "redirect:/cita/" + codigo + "?error=fecha";
        }
        return "redirect:/cita/" + codigo + "/confirmada";
    }

    @GetMapping("/{codigo}/confirmada")
    public String confirmada(@PathVariable String codigo, Model model) {
        return service.buscarPorCodigo(codigo)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    return "cita-confirmada";
                })
                .orElse("redirect:/pre-evaluacion");
    }
}
