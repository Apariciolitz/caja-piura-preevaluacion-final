package com.cajapiura.preevaluacion.model;

/** Tipos de evento que se registran en la bitácora de auditoría. */
public enum AccionAuditoria {
    LOGIN_OK("Inicio de sesión"),
    LOGIN_FALLIDO("Intento de acceso fallido"),
    SOLICITUD_CREADA("Solicitud creada"),
    CAMBIO_ESTADO("Cambio de estado"),
    REASIGNACION("Reasignación de solicitud"),
    USUARIO_CREADO("Usuario creado"),
    USUARIO_ACTIVADO("Usuario reactivado"),
    USUARIO_DESACTIVADO("Usuario desactivado"),
    ROL_CAMBIADO("Cambio de rol"),
    PASSWORD_RESTABLECIDA("Contraseña restablecida"),
    PARAMETROS_ACTUALIZADOS("Parámetros actualizados"),
    EXPORTACION("Exportación de datos"),
    CONSULTA_BD("Consulta a la base de datos");

    private final String etiqueta;

    AccionAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
