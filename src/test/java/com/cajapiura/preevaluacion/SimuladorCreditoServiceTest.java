package com.cajapiura.preevaluacion;

import com.cajapiura.preevaluacion.service.SimuladorCreditoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba unitaria del simulador de cuotas (sistema de amortización de cuota fija).
 * Verifica que el cronograma tenga el número correcto de cuotas, que el saldo
 * cierre exactamente en cero al final del plazo, y que el total pagado sea
 * coherente con el monto más los intereses calculados.
 */
class SimuladorCreditoServiceTest {

    private final SimuladorCreditoService service = new SimuladorCreditoService();

    @Test
    void generaUnaCuotaPorCadaMesDelPlazo() {
        SimuladorCreditoService.ResultadoSimulacion resultado =
                service.simular(new BigDecimal("8000"), 18, new BigDecimal("24"));

        assertEquals(18, resultado.getCronograma().size());
    }

    @Test
    void elSaldoCierraEnCeroAlFinalDelPlazo() {
        SimuladorCreditoService.ResultadoSimulacion resultado =
                service.simular(new BigDecimal("5000"), 12, new BigDecimal("18"));

        BigDecimal saldoFinal = resultado.getCronograma()
                .get(resultado.getCronograma().size() - 1)
                .getSaldo();

        assertEquals(BigDecimal.ZERO.setScale(2), saldoFinal.setScale(2));
    }

    @Test
    void elTotalPagadoEsElMontoMasLosIntereses() {
        SimuladorCreditoService.ResultadoSimulacion resultado =
                service.simular(new BigDecimal("10000"), 24, new BigDecimal("20"));

        BigDecimal esperado = resultado.getMonto().add(resultado.getTotalIntereses());
        assertEquals(esperado.setScale(2), resultado.getTotalPagado().setScale(2));
    }

    @Test
    void conTasaCeroLaCuotaEsElMontoDivididoEntreElPlazo() {
        SimuladorCreditoService.ResultadoSimulacion resultado =
                service.simular(new BigDecimal("6000"), 12, BigDecimal.ZERO);

        assertTrue(resultado.getCuotaMensual().compareTo(new BigDecimal("499")) > 0);
        assertTrue(resultado.getCuotaMensual().compareTo(new BigDecimal("501")) < 0);
    }
}
