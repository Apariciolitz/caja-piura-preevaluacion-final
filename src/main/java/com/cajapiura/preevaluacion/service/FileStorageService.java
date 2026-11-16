package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.DocumentoAdjunto;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.repository.DocumentoAdjuntoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Guarda en disco los documentos que el cliente adjunta (DNI, sustento de
 * ingresos) y registra sus metadatos en base de datos. Reemplaza la
 * simulación de carga de la primera versión por una carga real y
 * descargable por el asesor.
 */
@Service
public class FileStorageService {

    private final DocumentoAdjuntoRepository repository;
    private final Path raiz;

    public FileStorageService(DocumentoAdjuntoRepository repository,
                               @Value("${app.uploads.dir:./uploads}") String directorio) {
        this.repository = repository;
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de documentos: " + raiz, e);
        }
    }

    /** Guarda el archivo si no está vacío; si el campo viene vacío, no hace nada (los documentos son opcionales). */
    public void guardarSiPresente(MultipartFile archivo, SolicitudCredito solicitud, String tipoDocumento) {
        if (archivo == null || archivo.isEmpty()) {
            return;
        }
        try {
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String nombreAlmacenado = UUID.randomUUID() + extension;
            Path destino = raiz.resolve(nombreAlmacenado);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            DocumentoAdjunto documento = new DocumentoAdjunto();
            documento.setSolicitud(solicitud);
            documento.setTipoDocumento(tipoDocumento);
            documento.setNombreOriginal(archivo.getOriginalFilename());
            documento.setNombreAlmacenado(nombreAlmacenado);
            documento.setContentType(archivo.getContentType());
            documento.setTamanioBytes(archivo.getSize());
            repository.save(documento);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el documento adjunto", e);
        }
    }

    public List<DocumentoAdjunto> documentosDe(SolicitudCredito solicitud) {
        return repository.findBySolicitudOrderByFechaCargaAsc(solicitud);
    }

    public Path rutaFisicaDe(DocumentoAdjunto documento) {
        return raiz.resolve(documento.getNombreAlmacenado());
    }

    public DocumentoAdjunto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.'));
    }
}
