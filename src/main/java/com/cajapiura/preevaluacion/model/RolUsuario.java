package com.cajapiura.preevaluacion.model;

/** Rol del usuario interno dentro del panel de Caja Piura. */
public enum RolUsuario {
    ASESOR("Asesor de negocios"),
    ADMIN("Administrador");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
