package com.cajapiura.preevaluacion.controller;

import com.cajapiura.preevaluacion.model.DocumentoAdjunto;
import com.cajapiura.preevaluacion.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Descarga de documentos adjuntos (DNI, sustento de ingresos), accesible
 * solo para el asesor autenticado: la ruta vive bajo /asesor/**, que ya
 * está protegida por Spring Security.
 */
@RestController
@RequestMapping("/asesor/documentos")
public class DocumentoController {

    private final FileStorageService fileStorageService;

    public DocumentoController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        DocumentoAdjunto documento = fileStorageService.buscarPorId(id);
        Path ruta = fileStorageService.rutaFisicaDe(documento);

        if (!Files.exists(ruta)) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new FileSystemResource(ruta);
        MediaType tipo = documento.getContentType() != null
                ? MediaType.parseMediaType(documento.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment()
                        .filename(documento.getNombreOriginal()).build().toString())
                .body(recurso);
    }
}
