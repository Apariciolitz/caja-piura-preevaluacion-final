package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Metadatos de un archivo subido por el cliente (DNI o sustento de
 * ingresos). El archivo en sí se guarda en disco (ver FileStorageService);
 * aquí solo se referencia el nombre guardado para poder recuperarlo.
 */
@Entity
@Table(name = "documentos_adjuntos")
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudCredito solicitud;

    private String tipoDocumento;

    private String nombreOriginal;

    private String nombreAlmacenado;

    private String contentType;

    private long tamanioBytes;

    private LocalDateTime fechaCarga = LocalDateTime.now();

    public DocumentoAdjunto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SolicitudCredito getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudCredito solicitud) {
        this.solicitud = solicitud;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    /** Tamaño legible, ej. "1.2 MB", para mostrar en la interfaz. */
    @Transient
    public String getTamanioLegible() {
        if (tamanioBytes < 1024) return tamanioBytes + " B";
        if (tamanioBytes < 1024 * 1024) return String.format("%.1f KB", tamanioBytes / 1024.0);
        return String.format("%.1f MB", tamanioBytes / (1024.0 * 1024));
    }
}
