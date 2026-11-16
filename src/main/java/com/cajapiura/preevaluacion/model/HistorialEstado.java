package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría: cada cambio de estado de una solicitud queda
 * grabado con fecha y quién lo hizo. Es la base de la línea de tiempo /
 * secuencia de los procesos principales que se muestra en el detalle
 * de cada solicitud.
 */
@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudCredito solicitud;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estadoNuevo;

    /** EAGER: se muestra en la línea de tiempo del detalle, fuera de la transacción (open-in-view=false). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 300)
    private String detalle;

    private LocalDateTime fecha = LocalDateTime.now();

    public HistorialEstado() {
    }

    public HistorialEstado(SolicitudCredito solicitud, EstadoSolicitud estadoAnterior,
                            EstadoSolicitud estadoNuevo, Usuario usuario, String detalle) {
        this.solicitud = solicitud;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.detalle = detalle;
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

    public EstadoSolicitud getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoSolicitud estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoSolicitud getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoSolicitud estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
