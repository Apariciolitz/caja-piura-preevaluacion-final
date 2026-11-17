package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.*;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import com.cajapiura.preevaluacion.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consola de administración: supervisión global, KPIs, usuarios, parámetros y auditoría. Solo ROLE_ADMIN. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int DIAS_ESTANCADA = 3;

    private final PreEvaluacionService service;
    private final UsuarioRepository usuarioRepository;
    private final AdminUsuarioService usuarioService;
    private final ParametroService parametros;
    private final AuditoriaService auditoria;

    public AdminController(PreEvaluacionService service, UsuarioRepository usuarioRepository,
                           AdminUsuarioService usuarioService, ParametroService parametros,
                           AuditoriaService auditoria) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    private static String quien(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    private Usuario usuarioActual(Principal principal) {
        return principal == null ? null : usuarioRepository.findByUsernameIgnoreCase(principal.getName()).orElse(null);
    }

    // ---------------------------------------------------------------- Dashboard y KPIs

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        List<SolicitudCredito> solicitudes = service.listarTodas();
        model.addAttribute("kpi", KpiCalculator.global(solicitudes, DIAS_ESTANCADA, LocalDateTime.now()));
        model.addAttribute("kpisAsesor", KpiCalculator.porAsesor(usuarioRepository.findAll(), solicitudes));
        model.addAttribute("diasEstancada", DIAS_ESTANCADA);
        model.addAttribute("ultimosEventos", auditoria.buscar(null, null).stream().limit(8).toList());
        model.addAttribute("usuarioActual", usuarioActual(principal));
        model.addAttribute("activo", "dashboard");
        return "admin/dashboard";
    }

    // ---------------------------------------------------------------- Supervisión global

    @GetMapping("/supervision")
    public String supervision(@RequestParam(required = false) String q,
                              @RequestParam(required = false) EstadoSolicitud estado,
                              @RequestParam(required = false) ResultadoEvaluacion resultado,
                              @RequestParam(required = false) Long asesorId,
                              Model model, Principal principal) {
        model.addAttribute("solicitudes", service.buscarConFiltros(q, estado, resultado, asesorId));
        model.addAttribute("asesoresActivos", usuarioRepository.findByActivoTrueOrderByNombreCompletoAsc()
                .stream().filter(u -> u.getRol() == RolUsuario.ASESOR).toList());
        model.addAttribute("todosUsuarios", usuarioRepository.findAll());
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("resultados", ResultadoEvaluacion.values());
        model.addAttribute("filtroQ", q);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroResultado", resultado);
        model.addAttribute("filtroAsesorId", asesorId);
        model.addAttribute("usuarioActual", usuarioActual(principal));
        model.addAttribute("activo", "supervision");
        return "admin/supervision";
    }

    @PostMapping("/supervision/reasignar")
    public String reasignarVarias(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam Long asesorId,
                                  RedirectAttributes redirect, Principal principal) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("error", "Selecciona al menos una solicitud.");
            return "redirect:/admin/supervision";
        }
        Usuario actor = usuarioActual(principal);
        int ok = 0;
        try {
            for (Long id : ids) {
                service.reasignarAsesor(id, asesorId, actor);
                ok++;
            }
            redirect.addFlashAttribute("mensaje", ok + " solicitud(es) reasignada(s).");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage() + " (reasignadas antes del error: " + ok + ")");
        }
        return "redirect:/admin/supervision";
    }

    @GetMapping("/supervision/exportar")
    public void exportar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) EstadoSolicitud estado,
                         @RequestParam(required = false) ResultadoEvaluacion resultado,
                         @RequestParam(required = false) Long asesorId,
                         HttpServletResponse response, Principal principal) throws IOException {
        List<SolicitudCredito> lista = service.buscarConFiltros(q, estado, resultado, asesorId);
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(CsvUtil.fila(List.of("Código", "Solicitante", "DNI", "Ingreso mensual", "Monto solicitado",
                "Ratio", "Resultado", "Estado", "Asesor", "Fecha")));
        for (SolicitudCredito s : lista) {
            List<Object> fila = new ArrayList<>();
            fila.add("CP-" + String.format("%05d", s.getId()));
            fila.add(s.getNombreCompleto());
            fila.add(s.getDni());
            fila.add(s.getIngresoMensual());
            fila.add(s.getMontoSolicitado());
            fila.add(s.getRatio());
            fila.add(s.getResultado() == null ? "" : s.getResultado().getEtiqueta());
            fila.add(s.getEstado().getEtiqueta());
            fila.add(s.getAsesorAsignado() == null ? "Sin asignar" : s.getAsesorAsignado().getNombreCompleto());
            fila.add(s.getFechaCreacion());
            csv.append(CsvUtil.fila(fila));
        }
        auditoria.registrar(quien(principal), AccionAuditoria.EXPORTACION, "solicitudes", lista.size() + " filas");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"solicitudes.csv\"");
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- Usuarios

    @GetMapping("/usuarios")
    public String usuarios(Model model, Principal principal) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("roles", RolUsuario.values());
        model.addAttribute("usuarioActual", usuarioActual(principal));
        model.addAttribute("activo", "usuarios");
        return "admin/usuarios";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String username, @RequestParam String nombreCompleto,
                               @RequestParam RolUsuario rol, RedirectAttributes redirect, Principal principal) {
        try {
            var creado = usuarioService.crear(username, nombreCompleto, rol, quien(principal));
            redirect.addFlashAttribute("mensaje", "Usuario creado: " + creado.getUsuario().getUsername());
            redirect.addFlashAttribute("passwordTemporal", creado.getPasswordTemporal());
            redirect.addFlashAttribute("passwordDe", creado.getUsuario().getUsername());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/activo")
    public String cambiarActivo(@PathVariable Long id, @RequestParam boolean activo,
                                RedirectAttributes redirect, Principal principal) {
        try {
            Usuario u = usuarioService.cambiarActivo(id, activo, quien(principal));
            redirect.addFlashAttribute("mensaje", "Cuenta " + u.getUsername() + (activo ? " reactivada." : " desactivada."));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam RolUsuario rol,
                             RedirectAttributes redirect, Principal principal) {
        try {
            Usuario u = usuarioService.cambiarRol(id, rol, quien(principal));
            redirect.addFlashAttribute("mensaje", "Rol de " + u.getUsername() + " actualizado.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/restablecer")
    public String restablecer(@PathVariable Long id, RedirectAttributes redirect, Principal principal) {
        try {
            var r = usuarioService.restablecerPassword(id, quien(principal));
            redirect.addFlashAttribute("mensaje", "Contraseña restablecida para " + r.getUsuario().getUsername());
            redirect.addFlashAttribute("passwordTemporal", r.getPasswordTemporal());
            redirect.addFlashAttribute("passwordDe", r.getUsuario().getUsername());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ---------------------------------------------------------------- Parámetros

    @GetMapping("/parametros")
    public String verParametros(Model model, Principal principal) {
        model.addAttribute("parametros", parametros.listar());
        model.addAttribute("usuarioActual", usuarioActual(principal));
        model.addAttribute("activo", "parametros");
        return "admin/parametros";
    }

    @PostMapping("/parametros")
    public String guardarParametros(@RequestParam Map<String, String> params,
                                    RedirectAttributes redirect, Principal principal) {
        Map<String, String> propuestos = new LinkedHashMap<>();
        for (String clave : ParametroService.definicion().keySet()) {
            propuestos.put(clave, params.get(clave));
        }
        List<String> errores = ParametroService.validar(propuestos);
        if (!errores.isEmpty()) {
            redirect.addFlashAttribute("error", String.join(". ", errores) + ".");
            return "redirect:/admin/parametros";
        }
        int cambios = parametros.actualizar(propuestos, quien(principal));
        redirect.addFlashAttribute("mensaje", cambios == 0 ? "No había cambios que guardar."
                : cambios + " parámetro(s) actualizado(s). Se aplican a las nuevas solicitudes.");
        return "redirect:/admin/parametros";
    }

    // ---------------------------------------------------------------- Auditoría

    @GetMapping("/auditoria")
    public String verAuditoria(@RequestParam(required = false) String usuario,
                               @RequestParam(required = false) AccionAuditoria accion,
                               Model model, Principal principal) {
        model.addAttribute("eventos", auditoria.buscar(usuario, accion));
        model.addAttribute("acciones", AccionAuditoria.values());
        model.addAttribute("filtroUsuario", usuario);
        model.addAttribute("filtroAccion", accion);
        model.addAttribute("usuarioActual", usuarioActual(principal));
        model.addAttribute("activo", "auditoria");
        return "admin/auditoria";
    }
}
