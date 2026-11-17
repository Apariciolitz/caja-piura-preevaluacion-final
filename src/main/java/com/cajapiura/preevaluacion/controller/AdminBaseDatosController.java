package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.service.AuditoriaService;
import com.cajapiura.preevaluacion.service.BaseDatosService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

/** Acceso del administrador a la base de datos (solo lectura, con auditoría). */
@Controller
@RequestMapping("/admin/base-datos")
public class AdminBaseDatosController {

    private final BaseDatosService baseDatos;
    private final AuditoriaService auditoria;
    private final boolean consolaH2;

    public AdminBaseDatosController(BaseDatosService baseDatos, AuditoriaService auditoria,
                                    @Value("${spring.h2.console.enabled:false}") boolean consolaH2) {
        this.baseDatos = baseDatos;
        this.auditoria = auditoria;
        this.consolaH2 = consolaH2;
    }

    @GetMapping
    public String indice(Model model) {
        model.addAttribute("tablas", baseDatos.tablas());
        model.addAttribute("motor", baseDatos.motor());
        model.addAttribute("consolaH2", consolaH2);
        model.addAttribute("activo", "base-datos");
        return "admin/base-datos";
    }

    @GetMapping("/{tabla}")
    public String tabla(@PathVariable String tabla, @RequestParam(defaultValue = "1") int pagina,
                        Model model, Principal principal, RedirectAttributes redirect) {
        try {
            var datos = baseDatos.pagina(tabla, pagina);
            auditoria.registrar(principal == null ? null : principal.getName(), AccionAuditoria.CONSULTA_BD,
                    "tabla:" + datos.getTabla(), "Página " + datos.getPagina());
            model.addAttribute("datos", datos);
            model.addAttribute("activo", "base-datos");
            return "admin/base-datos-tabla";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/base-datos";
        }
    }

    @GetMapping("/{tabla}/exportar")
    public void exportar(@PathVariable String tabla, HttpServletResponse response, Principal principal)
            throws IOException {
        String csv;
        try {
            csv = baseDatos.exportarCsv(tabla);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "La tabla no existe");
            return;
        }
        auditoria.registrar(principal == null ? null : principal.getName(), AccionAuditoria.EXPORTACION,
                "tabla:" + tabla, "CSV de la tabla");
        String nombreSeguro = tabla.replaceAll("[^A-Za-z0-9_]", "");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreSeguro + ".csv\"");
        response.getOutputStream().write(("﻿" + csv).getBytes(StandardCharsets.UTF_8));
    }
}
