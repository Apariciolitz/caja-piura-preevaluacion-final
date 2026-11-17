package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.EstadoSolicitud;
import com.cajapiura.preevaluacion.model.ResultadoEvaluacion;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import com.cajapiura.preevaluacion.model.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cálculo de KPIs globales y por asesor. Lógica pura (sin base de datos) para poder probarla. */
public final class KpiCalculator {

    private KpiCalculator() {
    }

    /** KPIs de un asesor. Clase con getters (no record) para que Thymeleaf/SpEL los lea sin problemas. */
    public static class KpiAsesor {
        private final Long asesorId;
        private final String nombre;
        private final boolean activo;
        private final long asignadas;
        private final long activas;
        private final long cerradas;
        private final long pendientes;
        private final long precalificadas;
        private final long enRevision;
        private final BigDecimal montoGestionado;
        private final double tasaCierre;

        public KpiAsesor(Long asesorId, String nombre, boolean activo, long asignadas, long activas, long cerradas,
                         long pendientes, long precalificadas, long enRevision, BigDecimal montoGestionado) {
            this.asesorId = asesorId;
            this.nombre = nombre;
            this.activo = activo;
            this.asignadas = asignadas;
            this.activas = activas;
            this.cerradas = cerradas;
            this.pendientes = pendientes;
            this.precalificadas = precalificadas;
            this.enRevision = enRevision;
            this.montoGestionado = montoGestionado;
            this.tasaCierre = asignadas == 0 ? 0.0 : redondear(100.0 * cerradas / asignadas);
        }

        public Long getAsesorId() { return asesorId; }
        public String getNombre() { return nombre; }
        public boolean isActivo() { return activo; }
        public long getAsignadas() { return asignadas; }
        public long getActivas() { return activas; }
        public long getCerradas() { return cerradas; }
        public long getPendientes() { return pendientes; }
        public long getPrecalificadas() { return precalificadas; }
        public long getEnRevision() { return enRevision; }
        public BigDecimal getMontoGestionado() { return montoGestionado; }
        public double getTasaCierre() { return tasaCierre; }
    }

    /** KPIs globales de la cartera de solicitudes. */
    public static class KpiGlobal {
        private final long total;
        private final long precalificadas;
        private final long enRevision;
        private final long pendientes;
        private final long cerradas;
        private final long sinAsesor;
        private final long estancadas;
        private final double tasaPrecalificacion;
        private final double tasaCierre;
        private final BigDecimal montoTotal;
        private final BigDecimal montoPromedio;

        public KpiGlobal(long total, long precalificadas, long enRevision, long pendientes, long cerradas,
                         long sinAsesor, long estancadas, BigDecimal montoTotal) {
            this.total = total;
            this.precalificadas = precalificadas;
            this.enRevision = enRevision;
            this.pendientes = pendientes;
            this.cerradas = cerradas;
            this.sinAsesor = sinAsesor;
            this.estancadas = estancadas;
            this.montoTotal = montoTotal;
            this.tasaPrecalificacion = total == 0 ? 0.0 : redondear(100.0 * precalificadas / total);
            this.tasaCierre = total == 0 ? 0.0 : redondear(100.0 * cerradas / total);
            this.montoPromedio = total == 0 ? BigDecimal.ZERO
                    : montoTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        public long getTotal() { return total; }
        public long getPrecalificadas() { return precalificadas; }
        public long getEnRevision() { return enRevision; }
        public long getPendientes() { return pendientes; }
        public long getCerradas() { return cerradas; }
        public long getSinAsesor() { return sinAsesor; }
        public long getEstancadas() { return estancadas; }
        public double getTasaPrecalificacion() { return tasaPrecalificacion; }
        public double getTasaCierre() { return tasaCierre; }
        public BigDecimal getMontoTotal() { return montoTotal; }
        public BigDecimal getMontoPromedio() { return montoPromedio; }
    }

    static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    /**
     * @param diasEstancada una solicitud PENDIENTE con más de estos días sin atenderse se considera estancada
     */
    public static KpiGlobal global(List<SolicitudCredito> solicitudes, int diasEstancada, LocalDateTime ahora) {
        long pre = 0, rev = 0, pend = 0, cerr = 0, sinAsesor = 0, estancadas = 0;
        BigDecimal monto = BigDecimal.ZERO;
        for (SolicitudCredito s : solicitudes) {
            if (s.getResultado() == ResultadoEvaluacion.PRECALIFICADO) pre++;
            if (s.getResultado() == ResultadoEvaluacion.REQUIERE_REVISION) rev++;
            if (s.getEstado() == EstadoSolicitud.PENDIENTE) {
                pend++;
                if (s.getFechaCreacion() != null && s.getFechaCreacion().isBefore(ahora.minusDays(diasEstancada))) {
                    estancadas++;
                }
            }
            if (s.getEstado() == EstadoSolicitud.CERRADO) cerr++;
            if (s.getAsesorAsignado() == null) sinAsesor++;
            if (s.getMontoSolicitado() != null) monto = monto.add(s.getMontoSolicitado());
        }
        return new KpiGlobal(solicitudes.size(), pre, rev, pend, cerr, sinAsesor, estancadas, monto);
    }

    /** Una fila por usuario con rol de asesor o admin; ordenadas por solicitudes asignadas (mayor a menor). */
    public static List<KpiAsesor> porAsesor(List<Usuario> usuarios, List<SolicitudCredito> solicitudes) {
        List<KpiAsesor> filas = new ArrayList<>();
        for (Usuario u : usuarios) {
            long asignadas = 0, activas = 0, cerradas = 0, pendientes = 0, pre = 0, rev = 0;
            BigDecimal monto = BigDecimal.ZERO;
            for (SolicitudCredito s : solicitudes) {
                if (s.getAsesorAsignado() == null || !u.getId().equals(s.getAsesorAsignado().getId())) {
                    continue;
                }
                asignadas++;
                if (s.getEstado() == EstadoSolicitud.CERRADO) cerradas++; else activas++;
                if (s.getEstado() == EstadoSolicitud.PENDIENTE) pendientes++;
                if (s.getResultado() == ResultadoEvaluacion.PRECALIFICADO) pre++;
                if (s.getResultado() == ResultadoEvaluacion.REQUIERE_REVISION) rev++;
                if (s.getMontoSolicitado() != null) monto = monto.add(s.getMontoSolicitado());
            }
            filas.add(new KpiAsesor(u.getId(), u.getNombreCompleto(), u.isActivo(), asignadas, activas, cerradas,
                    pendientes, pre, rev, monto));
        }
        filas.sort(Comparator.comparingLong(KpiAsesor::getAsignadas).reversed()
                .thenComparing(KpiAsesor::getNombre));
        return filas;
    }
}
