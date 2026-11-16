package com.cajapiura.preevaluacion;

import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prueba unitaria de la regla de negocio central del proyecto:
 * ratio = montoSolicitado / ingresoMensual; ratio <= 4 => PRECALIFICADO.
 * (Documentada en el informe, sección II.2 - Representación funcional del sistema).
 */
class SolicitudCreditoTest {

    @Test
    void ratioMenorOIgualACuatroPrecalifica() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1800"));
        s.setMontoSolicitado(new BigDecimal("5600")); // ratio = 3.11

        s.evaluar();

        assertEquals(ResultadoEvaluacion.PRECALIFICADO, s.getResultado());
    }

    @Test
    void ratioMayorACuatroRequiereRevision() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("8000")); // ratio = 8.00

        s.evaluar();

        assertEquals(ResultadoEvaluacion.REQUIERE_REVISION, s.getResultado());
    }

    @Test
    void ratioExactamenteCuatroPrecalifica() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("4000")); // ratio = 4.00 (límite inclusivo)

        s.evaluar();

        assertEquals(ResultadoEvaluacion.PRECALIFICADO, s.getResultado());
    }
}
