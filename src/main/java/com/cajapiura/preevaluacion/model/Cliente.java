package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Cuenta de un cliente (persona que simula y solicita un crédito). Es independiente de {@link Usuario},
 * que es personal interno de Caja Piura (asesores y administradores).
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombreCompleto;

    private boolean activo = true;

    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** Momento en que la persona autorizó el tratamiento de sus datos personales (Ley N.° 29733). */
    private LocalDateTime consentimientoDatos;

    /** Versión de la política/términos que aceptó, para saber qué texto estaba vigente. */
    @Column(length = 20)
    private String versionTerminos;

    public Cliente() {
    }

    public Cliente(String correo, String passwordHash, String nombreCompleto) {
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.nombreCompleto = nombreCompleto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getConsentimientoDatos() { return consentimientoDatos; }
    public void setConsentimientoDatos(LocalDateTime consentimientoDatos) { this.consentimientoDatos = consentimientoDatos; }
    public String getVersionTerminos() { return versionTerminos; }
    public void setVersionTerminos(String versionTerminos) { this.versionTerminos = versionTerminos; }
}
