package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Usuario interno (asesor de negocios o administrador) que puede ingresar
 * al panel privado. Reemplaza al usuario único en memoria de la primera
 * versión, para soportar varios asesores trabajando en equipo
 * (colaboración: asignación de solicitudes y comentarios internos).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 60)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Column(length = 120)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    private RolUsuario rol = RolUsuario.ASESOR;

    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(String username, String passwordHash, String nombreCompleto, RolUsuario rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
