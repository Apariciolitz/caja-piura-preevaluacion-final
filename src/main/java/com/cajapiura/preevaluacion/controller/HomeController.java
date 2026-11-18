package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("solicitud", new SolicitudCredito());
        return "landing";
    }

    @GetMapping("/privacidad")
    public String privacidad(Model model) {
        model.addAttribute("version", com.cajapiura.preevaluacion.service.ClienteValidador.VERSION_TERMINOS);
        return "privacidad";
    }
}
