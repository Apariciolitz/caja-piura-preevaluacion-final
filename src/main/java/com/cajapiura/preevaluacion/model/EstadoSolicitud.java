package com.cajapiura.preevaluacion.model;

/** Estado del expediente dentro del flujo de atencion del asesor de negocios. */
public enum EstadoSolicitud {
    PENDIENTE("Pendiente"),
    CITA_AGENDADA("Cita agendada"),
    EN_REVISION("En revisión"),
    CONTACTADO("Contactado"),
    CERRADO("Cerrado");

    private final String etiqueta;

    EstadoSolicitud(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
