package com.cajapiura.preevaluacion.model;

/**
 * Resultado posible de la pre-evaluacion automatica.
 * Regla de negocio:
 *   ratio = montoSolicitado / ingresoMensual
 *   ratio <= 4  -> PRECALIFICADO
 *   ratio  > 4  -> REQUIERE_REVISION
 */
public enum ResultadoEvaluacion {

    PRECALIFICADO("Pre-calificado", "Buen punto de partida"),
    REQUIERE_REVISION("Requiere revisión", "Un asesor te contactará con alternativas");

    private final String etiqueta;
    private final String subtitulo;

    ResultadoEvaluacion(String etiqueta, String subtitulo) {
        this.etiqueta = etiqueta;
        this.subtitulo = subtitulo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getSubtitulo() {
        return subtitulo;
    }
}
