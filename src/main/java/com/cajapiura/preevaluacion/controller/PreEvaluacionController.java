package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.service.ClienteService;
import com.cajapiura.preevaluacion.service.FileStorageService;
import com.cajapiura.preevaluacion.service.PreEvaluacionService;
import com.cajapiura.preevaluacion.service.ValidadorArchivos;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Controller
@RequestMapping("/pre-evaluacion")
public class PreEvaluacionController {

    private final PreEvaluacionService service;
    private final FileStorageService fileStorageService;
    private final ClienteService clienteService;

    public PreEvaluacionController(PreEvaluacionService service, FileStorageService fileStorageService,
                                   ClienteService clienteService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
        this.clienteService = clienteService;
    }

    /** El cliente no puede fijar por formulario campos que calcula o asigna el sistema. */
    @InitBinder("solicitud")
    public void restringirCampos(WebDataBinder binder) {
        binder.setDisallowedFields("id", "cliente*", "estado", "resultado", "ratio", "asesorAsignado*",
                "codigoPublico", "fechaCreacion");
    }

    @GetMapping
    public String formulario(Model model, Principal principal) {
        if (!model.containsAttribute("solicitud")) {
            SolicitudCredito nueva = new SolicitudCredito();
            // Se precargan los datos de la cuenta para que el cliente no los repita.
            clienteService.buscarPorCorreo(principal.getName()).ifPresent(c -> {
                nueva.setNombreCompleto(c.getNombreCompleto());
                nueva.setCorreo(c.getCorreo());
            });
            model.addAttribute("solicitud", nueva);
        }
        return "pre-evaluacion";
    }

    @PostMapping
    public String procesar(@Valid @ModelAttribute("solicitud") SolicitudCredito solicitud,
                            BindingResult bindingResult,
                            @RequestParam(value = "documentoDni", required = false) MultipartFile documentoDni,
                            @RequestParam(value = "documentoSustento", required = false) MultipartFile documentoSustento,
                            Model model, Principal principal) {
        // Los documentos se validan ANTES de guardar la solicitud (INC-08).
        String errorDocumentos = validarDocumento(documentoDni, "DNI");
        if (errorDocumentos == null) {
            errorDocumentos = validarDocumento(documentoSustento, "Sustento de ingresos");
        }
        if (errorDocumentos != null) {
            model.addAttribute("errorDocumentos", errorDocumentos);
            return "pre-evaluacion";
        }
        if (bindingResult.hasErrors()) {
            return "pre-evaluacion";
        }
        clienteService.buscarPorCorreo(principal.getName()).ifPresent(solicitud::setCliente);
        SolicitudCredito guardada = service.evaluarYGuardar(solicitud);

        fileStorageService.guardarSiPresente(documentoDni, guardada, "DNI");
        fileStorageService.guardarSiPresente(documentoSustento, guardada, "Sustento de ingresos");

        return "redirect:/resultado/" + guardada.getCodigoPublico();
    }

    private String validarDocumento(MultipartFile archivo, String nombreCampo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        return ValidadorArchivos
                .validar(archivo.getOriginalFilename(), archivo.getContentType(), archivo.getSize())
                .map(mensaje -> nombreCampo + ": " + mensaje)
                .orElse(null);
    }
}
