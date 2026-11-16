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

    @GetMapping("/{id}")
    public String formulario(@PathVariable Long id, Model model) {
        return service.buscarPorId(id)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    model.addAttribute("fechaMinima", LocalDate.now().plusDays(1));
                    return "cita";
                })
                .orElse("redirect:/pre-evaluacion");
    }

    @PostMapping("/{id}")
    public String agendar(@PathVariable Long id,
                           @RequestParam String fecha,
                           @RequestParam String hora,
                           @RequestParam String agencia) {
        service.agendarCita(id, LocalDate.parse(fecha), hora, agencia);
        return "redirect:/cita/" + id + "/confirmada";
    }

    @GetMapping("/{id}/confirmada")
    public String confirmada(@PathVariable Long id, Model model) {
        return service.buscarPorId(id)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    return "cita-confirmada";
                })
                .orElse("redirect:/pre-evaluacion");
    }
}
