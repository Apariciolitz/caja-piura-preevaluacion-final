package com.cajapiura.preevaluacion;

import com.cajapiura.preevaluacion.model.*;
import com.cajapiura.preevaluacion.service.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias (sin base de datos) del módulo de administración. */
class AdminModuloTest {

    private static Usuario usuario(long id, String nombre, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario("u" + id, "hash", nombre, rol);
        u.setId(id);
        u.setActivo(activo);
        return u;
    }

    private static SolicitudCredito solicitud(Usuario asesor, EstadoSolicitud estado, String ingreso, String monto,
                                              LocalDateTime fecha) {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal(ingreso));
        s.setMontoSolicitado(new BigDecimal(monto));
        s.evaluar();
        s.setEstado(estado);
        s.setAsesorAsignado(asesor);
        s.setFechaCreacion(fecha);
        return s;
    }

    // ---- Regla de negocio con límite configurable

    @Test
    void ratioLimiteConfigurableCambiaElResultado() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("5000")); // ratio 5.00

        s.evaluar(new BigDecimal("6"));
        assertEquals(ResultadoEvaluacion.PRECALIFICADO, s.getResultado());

        s.evaluar(new BigDecimal("4"));
        assertEquals(ResultadoEvaluacion.REQUIERE_REVISION, s.getResultado());
    }

    @Test
    void limiteInvalidoSeRechaza() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("1000"));
        assertThrows(IllegalArgumentException.class, () -> s.evaluar(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> s.evaluar(null));
    }

    @Test
    void ratioRedondeadoNoOcultaQueSuperaElLimiteConfigurado() {
        SolicitudCredito s = new SolicitudCredito();
        s.setIngresoMensual(new BigDecimal("1000"));
        s.setMontoSolicitado(new BigDecimal("5001")); // 5.001 con límite 5
        s.evaluar(new BigDecimal("5"));
        assertEquals(ResultadoEvaluacion.REQUIERE_REVISION, s.getResultado());
        assertEquals(new BigDecimal("5.01"), s.getRatio());
    }

    // ---- KPIs

    @Test
    void kpisPorAsesorCuentanSoloSusCasos() {
        Usuario ana = usuario(1, "Ana", RolUsuario.ASESOR, true);
        Usuario beto = usuario(2, "Beto", RolUsuario.ASESOR, true);
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 10, 12, 0);
        List<SolicitudCredito> lista = List.of(
                solicitud(ana, EstadoSolicitud.CERRADO, "2000", "4000", ahora),
                solicitud(ana, EstadoSolicitud.PENDIENTE, "1000", "9000", ahora),
                solicitud(beto, EstadoSolicitud.EN_REVISION, "3000", "3000", ahora));

        var filas = KpiCalculator.porAsesor(List.of(ana, beto), lista);

        assertEquals(2, filas.size());
        var filaAna = filas.get(0); // más asignadas primero
        assertEquals("Ana", filaAna.getNombre());
        assertEquals(2, filaAna.getAsignadas());
        assertEquals(1, filaAna.getCerradas());
        assertEquals(1, filaAna.getActivas());
        assertEquals(1, filaAna.getPrecalificadas());
        assertEquals(1, filaAna.getEnRevision());
        assertEquals(50.0, filaAna.getTasaCierre());
        assertEquals(new BigDecimal("13000"), filaAna.getMontoGestionado());
        assertEquals(1, filas.get(1).getAsignadas());
    }

    @Test
    void kpisSinDatosNoDividenPorCero() {
        var global = KpiCalculator.global(List.of(), 3, LocalDateTime.now());
        assertEquals(0, global.getTotal());
        assertEquals(0.0, global.getTasaPrecalificacion());
        assertEquals(BigDecimal.ZERO, global.getMontoPromedio());

        var filas = KpiCalculator.porAsesor(List.of(usuario(1, "Ana", RolUsuario.ASESOR, true)), List.of());
        assertEquals(0.0, filas.get(0).getTasaCierre());
    }

    @Test
    void kpiGlobalDetectaEstancadasYSinAsesor() {
        Usuario ana = usuario(1, "Ana", RolUsuario.ASESOR, true);
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 10, 12, 0);
        List<SolicitudCredito> lista = List.of(
                solicitud(ana, EstadoSolicitud.PENDIENTE, "2000", "4000", ahora.minusDays(5)),   // estancada
                solicitud(ana, EstadoSolicitud.PENDIENTE, "2000", "4000", ahora.minusDays(1)),   // reciente
                solicitud(null, EstadoSolicitud.CERRADO, "1000", "9000", ahora.minusDays(10)));  // sin asesor

        var g = KpiCalculator.global(lista, 3, ahora);

        assertEquals(3, g.getTotal());
        assertEquals(2, g.getPendientes());
        assertEquals(1, g.getEstancadas());
        assertEquals(1, g.getSinAsesor());
        assertEquals(1, g.getCerradas());
        assertEquals(66.7, g.getTasaPrecalificacion());
        assertEquals(new BigDecimal("17000"), g.getMontoTotal());
    }

    // ---- Parámetros

    private static Map<String, String> validos() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(ParametroService.RATIO_LIMITE, "4");
        m.put(ParametroService.TASA_DEFECTO, "24");
        m.put(ParametroService.MONTO_MIN, "500");
        m.put(ParametroService.MONTO_MAX, "200000");
        m.put(ParametroService.PLAZO_MIN, "3");
        m.put(ParametroService.PLAZO_MAX, "60");
        return m;
    }

    @Test
    void parametrosPorDefectoSonValidos() {
        assertTrue(ParametroService.validar(validos()).isEmpty());
    }

    @Test
    void parametrosFueraDeRangoSeRechazan() {
        Map<String, String> m = validos();
        m.put(ParametroService.RATIO_LIMITE, "50");
        assertFalse(ParametroService.validar(m).isEmpty());

        m = validos();
        m.put(ParametroService.TASA_DEFECTO, "-1");
        assertFalse(ParametroService.validar(m).isEmpty());

        m = validos();
        m.put(ParametroService.MONTO_MIN, "300000"); // mínimo mayor al máximo
        assertFalse(ParametroService.validar(m).isEmpty());

        m = validos();
        m.put(ParametroService.PLAZO_MAX, "2"); // máximo menor al mínimo
        assertFalse(ParametroService.validar(m).isEmpty());

        m = validos();
        m.put(ParametroService.PLAZO_MIN, "3.5"); // no entero
        assertFalse(ParametroService.validar(m).isEmpty());
    }

    @Test
    void parametrosNoNumericosOFaltantesSeRechazan() {
        Map<String, String> m = validos();
        m.put(ParametroService.RATIO_LIMITE, "abc");
        assertFalse(ParametroService.validar(m).isEmpty());

        m = validos();
        m.remove(ParametroService.MONTO_MAX);
        assertFalse(ParametroService.validar(m).isEmpty());
    }

    // ---- Usuarios

    @Test
    void validaNombresDeUsuario() {
        assertTrue(AdminUsuarioService.usernameValido("mgarcia"));
        assertTrue(AdminUsuarioService.usernameValido("m.garcia-2"));
        assertFalse(AdminUsuarioService.usernameValido("ab"));
        assertFalse(AdminUsuarioService.usernameValido("MGarcia"));
        assertFalse(AdminUsuarioService.usernameValido("mi usuario"));
        assertFalse(AdminUsuarioService.usernameValido("a'; DROP TABLE usuarios;--"));
        assertFalse(AdminUsuarioService.usernameValido(null));
    }

    @Test
    void contrasenaTemporalEsLargaYDistinta() {
        String a = AdminUsuarioService.generarPassword();
        String b = AdminUsuarioService.generarPassword();
        assertEquals(12, a.length());
        assertNotEquals(a, b);
        assertTrue(a.matches("[A-Za-z0-9]+"));
    }

    // ---- CSV y base de datos

    @Test
    void csvEscapaYNeutralizaFormulas() {
        assertEquals("simple", CsvUtil.celda("simple"));
        assertEquals("\"con,coma\"", CsvUtil.celda("con,coma"));
        assertEquals("\"dice \"\"hola\"\"\"", CsvUtil.celda("dice \"hola\""));
        assertEquals("'=SUM(A1:A2)", CsvUtil.celda("=SUM(A1:A2)"));
        assertEquals("", CsvUtil.celda(null));
        assertEquals("a,b\r\n", CsvUtil.fila(List.of("a", "b")));
    }

    @Test
    void columnasSensiblesSeDetectan() {
        assertTrue(BaseDatosService.esSensible("PASSWORD_HASH"));
        assertTrue(BaseDatosService.esSensible("passwordHash"));
        assertFalse(BaseDatosService.esSensible("USERNAME"));
        assertFalse(BaseDatosService.esSensible(null));
    }

    @Test
    void auditoriaRecortaYLimpiaTexto() {
        assertEquals("a b", AuditoriaService.recortar("a\nb", 10));
        assertEquals(5, AuditoriaService.recortar("x".repeat(50), 5).length());
        assertNull(AuditoriaService.recortar(null, 5));
    }
}
