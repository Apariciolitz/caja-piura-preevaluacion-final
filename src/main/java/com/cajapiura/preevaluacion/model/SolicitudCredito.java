package com.cajapiura.preevaluacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de pre-evaluacion crediticia enviada
 * por un cliente potencial de Caja Piura desde el formulario web.
 */
@Entity
@Table(name = "solicitudes_credito")
public class SolicitudCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ingresa tu nombre completo")
    @Size(max = 120)
    private String nombreCompleto;

    @NotBlank(message = "Ingresa tu DNI")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotBlank(message = "Ingresa un número de contacto")
    @Size(max = 20)
    private String telefono;

    @Email(message = "Ingresa un correo válido")
    @Size(max = 150)
    private String correo;

    @NotNull(message = "Ingresa tu ingreso mensual")
    @DecimalMin(value = "1", message = "El ingreso mensual debe ser mayor a 0")
    @DecimalMax(value = "999999", message = "Ingresa un monto realista")
    @Column(precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @NotNull(message = "Ingresa el monto solicitado")
    @DecimalMin(value = "1", message = "El monto solicitado debe ser mayor a 0")
    @DecimalMax(value = "999999", message = "Ingresa un monto realista")
    @Column(precision = 12, scale = 2)
    private BigDecimal montoSolicitado;

    @NotBlank(message = "Selecciona el destino del crédito")
    private String destinoCredito;

    @Column(precision = 6, scale = 2)
    private BigDecimal ratio;

    @Enumerated(EnumType.STRING)
    private ResultadoEvaluacion resultado;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /**
     * Código público no adivinable (UUID) con el que el cliente accede a su
     * resultado, comprobante y cita. Reemplaza al ID secuencial en las URLs
     * públicas (incidente INC-02).
     */
    @Column(unique = true, length = 36)
    private String codigoPublico = java.util.UUID.randomUUID().toString();

    // Datos de la cita (opcional, se completan en el paso de agendamiento)
    private LocalDate citaFecha;
    private String citaHora;
    private String citaAgencia;

    @Column(length = 500)
    private String notasAsesor;

    /**
     * Asesor de negocios asignado para el seguimiento (colaboración de equipo).
     * EAGER a propósito: con open-in-view=false la sesión de Hibernate se
     * cierra antes de renderizar la vista, y varias plantillas (resultado,
     * tablero, kanban, detalle) muestran este dato fuera de la transacción.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_asignado_id")
    private Usuario asesorAsignado;

    public SolicitudCredito() {
    }

    /**
     * Calcula el ratio monto/ingreso y determina el resultado preliminar.
     * La decisión se toma con el ratio exacto (no el redondeado), para que un
     * caso como 4.004 no quede como PRECALIFICADO (INC-04). Si faltan datos,
     * falla de forma explícita en lugar de dejar el resultado vacío (INC-10).
     */
    public void evaluar() {
        evaluar(new BigDecimal("4"));
    }

    /** Igual que {@link #evaluar()} pero con el ratio límite configurable por el administrador. */
    public void evaluar(BigDecimal limite) {
        if (limite == null || limite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El ratio límite debe ser mayor a 0");
        }
        if (ingresoMensual == null || montoSolicitado == null
                || ingresoMensual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "No se puede evaluar: el ingreso mensual y el monto solicitado deben ser mayores a 0");
        }
        BigDecimal ratioExacto = montoSolicitado.divide(ingresoMensual, 10, RoundingMode.HALF_UP);
        boolean supera = ratioExacto.compareTo(limite) > 0;

        BigDecimal mostrado = ratioExacto.setScale(2, RoundingMode.HALF_UP);
        if (supera && mostrado.compareTo(limite) <= 0) {
            mostrado = limite.add(new BigDecimal("0.01")); // coherente con el resultado mostrado
        }
        this.ratio = mostrado;
        this.resultado = supera ? ResultadoEvaluacion.REQUIERE_REVISION : ResultadoEvaluacion.PRECALIFICADO;
    }

    // ----- Getters y setters -----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public BigDecimal getIngresoMensual() {
        return ingresoMensual;
    }

    public void setIngresoMensual(BigDecimal ingresoMensual) {
        this.ingresoMensual = ingresoMensual;
    }

    public BigDecimal getMontoSolicitado() {
        return montoSolicitado;
    }

    public void setMontoSolicitado(BigDecimal montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    public String getDestinoCredito() {
        return destinoCredito;
    }

    public void setDestinoCredito(String destinoCredito) {
        this.destinoCredito = destinoCredito;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }

    public ResultadoEvaluacion getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoEvaluacion resultado) {
        this.resultado = resultado;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public String getCodigoPublico() {
        return codigoPublico;
    }

    public void setCodigoPublico(String codigoPublico) {
        this.codigoPublico = codigoPublico;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDate getCitaFecha() {
        return citaFecha;
    }

    public void setCitaFecha(LocalDate citaFecha) {
        this.citaFecha = citaFecha;
    }

    public String getCitaHora() {
        return citaHora;
    }

    public void setCitaHora(String citaHora) {
        this.citaHora = citaHora;
    }

    public String getCitaAgencia() {
        return citaAgencia;
    }

    public void setCitaAgencia(String citaAgencia) {
        this.citaAgencia = citaAgencia;
    }

    public String getNotasAsesor() {
        return notasAsesor;
    }

    public void setNotasAsesor(String notasAsesor) {
        this.notasAsesor = notasAsesor;
    }

    public Usuario getAsesorAsignado() {
        return asesorAsignado;
    }

    public void setAsesorAsignado(Usuario asesorAsignado) {
        this.asesorAsignado = asesorAsignado;
    }
}
