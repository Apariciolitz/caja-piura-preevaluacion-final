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

    @Test
    void ratioRealMayorACuatroNoSeRedondeaAPrecalificado() {
        // INC-04: 4004 / 1000 = 4.004; antes se redondeaba a 4.00 y quedaba PRECALIFICADO.
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("4004"));

        s.evaluar();

        assertEquals(ResultadoEvaluacion.REQUIERE_REVISION, s.getResultado());
        assertEquals(new BigDecimal("4.01"), s.getRatio());
    }

    @Test
    void sinIngresoValidoLaEvaluacionFallaDeFormaExplicita() {
        // INC-10: antes dejaba el resultado en null sin avisar.
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(BigDecimal.ZERO);
        s.setMontoSolicitado(new BigDecimal("4000"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, s::evaluar);
    }

    @Test
    void cadaSolicitudTieneUnCodigoPublicoDistinto() {
        // INC-02: el cliente accede por código no adivinable, no por ID secuencial.
        assertEquals(36, new SolicitudCredito().getCodigoPublico().length());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                new SolicitudCredito().getCodigoPublico(), new SolicitudCredito().getCodigoPublico());
    }
}
