package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Comentario interno de un asesor sobre una solicitud. Permite que varios
 * asesores colaboren en un mismo caso dejando notas con autor y fecha
 * (herramienta de colaboración de equipo).
 */
@Entity
@Table(name = "comentarios_solicitud")
public class ComentarioSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudCredito solicitud;

    /** EAGER: se muestra en la lista de comentarios del detalle, fuera de la transacción (open-in-view=false). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @NotBlank
    @Column(length = 1000, nullable = false)
    private String texto;

    private LocalDateTime fecha = LocalDateTime.now();

    public ComentarioSolicitud() {
    }

    public ComentarioSolicitud(SolicitudCredito solicitud, Usuario autor, String texto) {
        this.solicitud = solicitud;
        this.autor = autor;
        this.texto = texto;
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

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
