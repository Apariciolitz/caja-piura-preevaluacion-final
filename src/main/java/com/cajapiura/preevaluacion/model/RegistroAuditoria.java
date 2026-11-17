package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Bitácora de auditoría: quién hizo qué y cuándo. Guarda el nombre de usuario como
 * texto (no como clave foránea) para que el registro se conserve aunque la cuenta se desactive.
 */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(length = 60)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccionAuditoria accion;

    @Column(length = 80)
    private String entidad;

    @Column(length = 400)
    private String detalle;

    public RegistroAuditoria() {
    }

    public RegistroAuditoria(String usuario, AccionAuditoria accion, String entidad, String detalle) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public AccionAuditoria getAccion() { return accion; }
    public void setAccion(AccionAuditoria accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
