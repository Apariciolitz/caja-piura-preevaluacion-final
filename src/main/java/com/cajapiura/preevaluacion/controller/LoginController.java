package com.cajapiura.preevaluacion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/asesor/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error != null);
        return "asesor/login";
    }

    /** Dirección fácil de recordar para el personal de Caja Piura. */
    @GetMapping("/colaboradores")
    public String colaboradores() {
        return "redirect:/asesor/login";
    }
}
