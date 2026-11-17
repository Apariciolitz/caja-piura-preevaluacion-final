package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Parámetro de negocio editable por el administrador (clave/valor). */
@Entity
@Table(name = "parametros_sistema")
public class ParametroSistema {

    @Id
    @Column(length = 60)
    private String clave;

    @Column(nullable = false, length = 100)
    private String valor;

    @Column(length = 250)
    private String descripcion;

    private LocalDateTime actualizado = LocalDateTime.now();

    @Column(length = 60)
    private String actualizadoPor;

    public ParametroSistema() {
    }

    public ParametroSistema(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getActualizado() { return actualizado; }
    public void setActualizado(LocalDateTime actualizado) { this.actualizado = actualizado; }
    public String getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(String actualizadoPor) { this.actualizadoPor = actualizadoPor; }
}
