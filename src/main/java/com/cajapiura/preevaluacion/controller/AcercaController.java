package com.cajapiura.preevaluacion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página pública que documenta el proceso y la arquitectura del proyecto. */
@Controller
public class AcercaController {

    @GetMapping("/acerca-del-proyecto")
    public String acerca() {
        return "acerca";
    }
}
