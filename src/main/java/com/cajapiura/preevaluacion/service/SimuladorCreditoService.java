package com.cajapiura.preevaluacion.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula la cuota mensual de un crédito (sistema de cuota fija / francés)
 * y genera su tabla de amortización completa. Es una herramienta pública
 * adicional a la pre-evaluación: permite al visitante simular distintos
 * escenarios de monto, plazo y tasa antes de solicitar su crédito.
 */
@Service
public class SimuladorCreditoService {

    public static class CuotaAmortizacion {
        public int numero;
        public BigDecimal cuota;
        public BigDecimal interes;
        public BigDecimal amortizacion;
        public BigDecimal saldo;

        public int getNumero() { return numero; }
        public BigDecimal getCuota() { return cuota; }
        public BigDecimal getInteres() { return interes; }
        public BigDecimal getAmortizacion() { return amortizacion; }
        public BigDecimal getSaldo() { return saldo; }
    }

    public static class ResultadoSimulacion {
        public BigDecimal monto;
        public int plazoMeses;
        public BigDecimal tasaAnualNominal;
        public BigDecimal cuotaMensual;
        public BigDecimal totalIntereses;
        public BigDecimal totalPagado;
        public List<CuotaAmortizacion> cronograma;

        public BigDecimal getMonto() { return monto; }
        public int getPlazoMeses() { return plazoMeses; }
        public BigDecimal getTasaAnualNominal() { return tasaAnualNominal; }
        public BigDecimal getCuotaMensual() { return cuotaMensual; }
        public BigDecimal getTotalIntereses() { return totalIntereses; }
        public BigDecimal getTotalPagado() { return totalPagado; }
        public List<CuotaAmortizacion> getCronograma() { return cronograma; }
    }

    /**
     * @param monto             monto del crédito en soles
     * @param plazoMeses        plazo en meses
     * @param tasaAnualNominal  tasa de interés anual nominal, en porcentaje (ej. 24 = 24% anual)
     */
    public ResultadoSimulacion simular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnualNominal) {
        // Validación en la propia capa de servicio (INC-11): no depende de que el controlador acote los datos.
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        if (plazoMeses < 1) {
            throw new IllegalArgumentException("El plazo debe ser de al menos 1 mes");
        }
        if (tasaAnualNominal == null || tasaAnualNominal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La tasa anual no puede ser negativa");
        }
        BigDecimal tasaMensual = tasaAnualNominal
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal cuota = calcularCuotaFija(monto, tasaMensual, plazoMeses);

        List<CuotaAmortizacion> cronograma = new ArrayList<>();
        BigDecimal saldo = monto;
        BigDecimal totalIntereses = BigDecimal.ZERO;

        for (int i = 1; i <= plazoMeses; i++) {
            BigDecimal interes = saldo.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amortizacion = cuota.subtract(interes).setScale(2, RoundingMode.HALF_UP);

            // En la última cuota se ajusta para que el saldo cierre exactamente en cero.
            if (i == plazoMeses) {
                amortizacion = saldo;
            }
            saldo = saldo.subtract(amortizacion).setScale(2, RoundingMode.HALF_UP);
            totalIntereses = totalIntereses.add(interes);

            CuotaAmortizacion fila = new CuotaAmortizacion();
            fila.numero = i;
            fila.cuota = (i == plazoMeses) ? amortizacion.add(interes) : cuota;
            fila.interes = interes;
            fila.amortizacion = amortizacion;
            fila.saldo = saldo.max(BigDecimal.ZERO);
            cronograma.add(fila);
        }

        ResultadoSimulacion resultado = new ResultadoSimulacion();
        resultado.monto = monto;
        resultado.plazoMeses = plazoMeses;
        resultado.tasaAnualNominal = tasaAnualNominal;
        resultado.cuotaMensual = cuota;
        resultado.totalIntereses = totalIntereses.setScale(2, RoundingMode.HALF_UP);
        resultado.totalPagado = monto.add(resultado.totalIntereses);
        resultado.cronograma = cronograma;
        return resultado;
    }

    /** Fórmula de cuota fija (sistema francés): C = P * i / (1 - (1+i)^-n) */
    private BigDecimal calcularCuotaFija(BigDecimal monto, BigDecimal tasaMensual, int plazoMeses) {
        double p = monto.doubleValue();
        double i = tasaMensual.doubleValue();
        double n = plazoMeses;

        if (i == 0) {
            return monto.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP);
        }

        double cuota = p * i / (1 - Math.pow(1 + i, -n));
        return BigDecimal.valueOf(cuota).setScale(2, RoundingMode.HALF_UP);
    }
}
