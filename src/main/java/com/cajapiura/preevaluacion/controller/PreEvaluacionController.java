package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.service.FileStorageService;
import com.cajapiura.preevaluacion.service.PreEvaluacionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/pre-evaluacion")
public class PreEvaluacionController {

    private final PreEvaluacionService service;
    private final FileStorageService fileStorageService;

    public PreEvaluacionController(PreEvaluacionService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String formulario(Model model) {
        if (!model.containsAttribute("solicitud")) {
            model.addAttribute("solicitud", new SolicitudCredito());
        }
        return "pre-evaluacion";
    }

    @PostMapping
    public String procesar(@Valid @ModelAttribute("solicitud") SolicitudCredito solicitud,
                            BindingResult bindingResult,
                            @RequestParam(value = "documentoDni", required = false) MultipartFile documentoDni,
                            @RequestParam(value = "documentoSustento", required = false) MultipartFile documentoSustento,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "pre-evaluacion";
        }
        SolicitudCredito guardada = service.evaluarYGuardar(solicitud);

        fileStorageService.guardarSiPresente(documentoDni, guardada, "DNI");
        fileStorageService.guardarSiPresente(documentoSustento, guardada, "Sustento de ingresos");

        return "redirect:/resultado/" + guardada.getId();
    }
}
