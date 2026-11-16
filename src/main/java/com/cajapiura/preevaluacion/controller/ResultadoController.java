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

    @GetMapping("/resultado/{id}")
    public String resultado(@PathVariable Long id, Model model) {
        return service.buscarPorId(id)
                .map(solicitud -> {
                    model.addAttribute("solicitud", solicitud);
                    return "resultado";
                })
                .orElse("redirect:/pre-evaluacion");
    }

    @GetMapping("/resultado/{id}/comprobante.pdf")
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id) {
        SolicitudCredito solicitud = service.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        byte[] pdf = pdfService.generarComprobante(solicitud);
        String nombreArchivo = "comprobante-CP-" + String.format("%05d", id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }
}
