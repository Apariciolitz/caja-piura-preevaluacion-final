package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.EstadoSolicitud;
import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.model.Usuario;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import com.cajapiura.preevaluacion.service.FileStorageService;
import com.cajapiura.preevaluacion.service.PreEvaluacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/asesor")
public class AsesorController {

    private final PreEvaluacionService service;
    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;

    public AsesorController(PreEvaluacionService service, UsuarioRepository usuarioRepository,
                             FileStorageService fileStorageService) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.fileStorageService = fileStorageService;
    }

    /** Resuelve el usuario interno (Usuario) a partir de la sesión autenticada. */
    private Usuario usuarioActual(Principal principal) {
        if (principal == null) {
            return null;
        }
        return usuarioRepository.findByUsernameIgnoreCase(principal.getName()).orElse(null);
    }

    @GetMapping
    public String tablero(@RequestParam(required = false) String q,
                           @RequestParam(required = false) EstadoSolicitud estado,
                           @RequestParam(required = false) ResultadoEvaluacion resultado,
                           @RequestParam(required = false) Long asesorId,
                           Model model, Principal principal) {

        List<SolicitudCredito> solicitudes = service.buscarConFiltros(q, estado, resultado, asesorId);

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("totalPrecalificados", service.contarPorResultado(ResultadoEvaluacion.PRECALIFICADO));
        model.addAttribute("totalRevision", service.contarPorResultado(ResultadoEvaluacion.REQUIERE_REVISION));
        model.addAttribute("totalSolicitudes", service.listarTodas().size());
        model.addAttribute("asesores", usuarioRepository.findByActivoTrueOrderByNombreCompletoAsc());
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("resultados", ResultadoEvaluacion.values());

        model.addAttribute("filtroQ", q);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroResultado", resultado);
        model.addAttribute("filtroAsesorId", asesorId);

        model.addAttribute("usuarioActual", usuarioActual(principal));
        return "asesor/tablero";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, Principal principal) {
        return service.buscarPorId(id)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    model.addAttribute("estados", EstadoSolicitud.values());
                    model.addAttribute("asesores", usuarioRepository.findByActivoTrueOrderByNombreCompletoAsc());
                    model.addAttribute("historial", service.historialDe(solicitud));
                    model.addAttribute("comentarios", service.comentariosDe(solicitud));
                    model.addAttribute("documentos", fileStorageService.documentosDe(solicitud));
                    model.addAttribute("usuarioActual", usuarioActual(principal));
                    return "asesor/detalle";
                })
                .orElse("redirect:/asesor");
    }

    @PostMapping("/{id}/estado")
    public String actualizarEstado(@PathVariable Long id,
                                    @RequestParam EstadoSolicitud estado,
                                    @RequestParam(required = false) String notas,
                                    Principal principal) {
        service.actualizarEstado(id, estado, notas, usuarioActual(principal));
        return "redirect:/asesor/" + id;
    }

    /** Variante sin redirección, usada por el tablero Kanban (drag and drop vía fetch). */
    @PostMapping("/{id}/estado-ajax")
    @ResponseBody
    public String actualizarEstadoAjax(@PathVariable Long id,
                                        @RequestParam EstadoSolicitud estado,
                                        Principal principal) {
        service.actualizarEstado(id, estado, null, usuarioActual(principal));
        return "ok";
    }

    @PostMapping("/{id}/reasignar")
    public String reasignar(@PathVariable Long id, @RequestParam Long asesorId) {
        service.reasignarAsesor(id, asesorId);
        return "redirect:/asesor/" + id;
    }

    @PostMapping("/{id}/comentarios")
    public String agregarComentario(@PathVariable Long id, @RequestParam String texto, Principal principal) {
        service.agregarComentario(id, usuarioActual(principal), texto);
        return "redirect:/asesor/" + id + "#comentarios";
    }

    @GetMapping("/kanban")
    public String kanban(Model model) {
        model.addAttribute("solicitudes", service.listarTodas());
        model.addAttribute("estados", EstadoSolicitud.values());
        return "asesor/kanban";
    }
}
