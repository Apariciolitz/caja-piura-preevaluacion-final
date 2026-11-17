package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.service.PdfService;
import com.cajapiura.preevaluacion.service.PreEvaluacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ResultadoController {

    private final PreEvaluacionService service;
    private final PdfService pdfService;

    public ResultadoController(PreEvaluacionService service, PdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/resultado/{codigo}")
    public String resultado(@PathVariable String codigo, Model model) {
        return service.buscarPorCodigo(codigo)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    return "resultado";
                })
                .orElse("redirect:/pre-evaluacion");
    }

    @GetMapping("/resultado/{codigo}/comprobante.pdf")
    public ResponseEntity<byte[]> comprobante(@PathVariable String codigo) {
        SolicitudCredito solicitud = service.buscarPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        Long id = solicitud.getId();

        byte[] pdf = pdfService.generarComprobante(solicitud);
        String nombreArchivo = "comprobante-CP-" + String.format("%05d", id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }
}
