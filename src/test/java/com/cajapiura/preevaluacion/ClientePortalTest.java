package com.cajapiura.preevaluacion;

import com.cajapiura.preevaluacion.service.ClienteValidador;
import com.cajapiura.preevaluacion.service.IntentosLogin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias (sin base de datos) del registro y acceso de clientes. */
class ClientePortalTest {

    private static final String OK_PASS = "Caja2026Segura";

    @Test
    void correosValidosEInvalidos() {
        assertTrue(ClienteValidador.correoValido("maria.perez@gmail.com"));
        assertTrue(ClienteValidador.correoValido("  MARIA@Ejemplo.COM "));
        assertFalse(ClienteValidador.correoValido("maria@"));
        assertFalse(ClienteValidador.correoValido("maria@ejemplo"));
        assertFalse(ClienteValidador.correoValido("sin arroba.com"));
        assertFalse(ClienteValidador.correoValido(null));
        assertEquals("maria@ejemplo.com", ClienteValidador.normalizarCorreo("  MARIA@Ejemplo.COM "));
    }

    @Test
    void politicaDeContrasena() {
        assertTrue(ClienteValidador.erroresPassword(OK_PASS, "a@b.com").isEmpty());
        assertFalse(ClienteValidador.erroresPassword("corta1A", "a@b.com").isEmpty());          // < 8
        assertFalse(ClienteValidador.erroresPassword("sinmayuscula1", "a@b.com").isEmpty());
        assertFalse(ClienteValidador.erroresPassword("SINMINUSCULA1", "a@b.com").isEmpty());
        assertFalse(ClienteValidador.erroresPassword("SinNumeroAqui", "a@b.com").isEmpty());
        assertFalse(ClienteValidador.erroresPassword("Password123", "a@b.com").isEmpty());      // común
        assertFalse(ClienteValidador.erroresPassword("Maria.perez1A", "maria.perez@gmail.com").isEmpty()); // contiene el correo
        assertFalse(ClienteValidador.erroresPassword("A1a" + "x".repeat(80), "a@b.com").isEmpty());          // > 72
    }

    @Test
    void registroValidoNoTieneErrores() {
        List<String> e = ClienteValidador.validarRegistro("María Fernanda Chunga", "maria@ejemplo.com",
                OK_PASS, OK_PASS, true, true);
        assertTrue(e.isEmpty(), e.toString());
    }

    @Test
    void registroExigeConsentimientoYConfirmacion() {
        assertFalse(ClienteValidador.validarRegistro("María Chunga", "m@ejemplo.com", OK_PASS, OK_PASS, false, true).isEmpty());
        assertFalse(ClienteValidador.validarRegistro("María Chunga", "m@ejemplo.com", OK_PASS, OK_PASS, true, false).isEmpty());
        assertFalse(ClienteValidador.validarRegistro("María Chunga", "m@ejemplo.com", OK_PASS, "Otra2026Clave", true, true).isEmpty());
    }

    @Test
    void registroRechazaNombresInvalidos() {
        assertFalse(ClienteValidador.validarRegistro("", "m@ejemplo.com", OK_PASS, OK_PASS, true, true).isEmpty());
        assertFalse(ClienteValidador.validarRegistro("<script>alert(1)</script>", "m@ejemplo.com", OK_PASS, OK_PASS, true, true).isEmpty());
        assertFalse(ClienteValidador.validarRegistro("12345", "m@ejemplo.com", OK_PASS, OK_PASS, true, true).isEmpty());
    }

    @Test
    void bloqueoTrasCincoFallosYSeLiberaConElTiempo() {
        IntentosLogin intentos = new IntentosLogin();
        Instant t0 = Instant.parse("2026-11-18T10:00:00Z");
        for (int i = 0; i < IntentosLogin.MAX_FALLOS - 1; i++) {
            intentos.registrarFallo("Maria@Ejemplo.com", t0.plusSeconds(i));
        }
        assertFalse(intentos.bloqueado("maria@ejemplo.com", t0.plusSeconds(10)));
        intentos.registrarFallo("maria@ejemplo.com", t0.plusSeconds(5));
        assertTrue(intentos.bloqueado("MARIA@ejemplo.com", t0.plusSeconds(10)));          // mismo correo, otra mayúscula
        assertFalse(intentos.bloqueado("otra@ejemplo.com", t0.plusSeconds(10)));          // otro correo no se afecta
        assertFalse(intentos.bloqueado("maria@ejemplo.com", t0.plus(IntentosLogin.VENTANA).plusSeconds(60))); // pasó la ventana
    }

    @Test
    void ingresoCorrectoLimpiaLosFallos() {
        IntentosLogin intentos = new IntentosLogin();
        Instant t0 = Instant.parse("2026-11-18T10:00:00Z");
        for (int i = 0; i < IntentosLogin.MAX_FALLOS; i++) {
            intentos.registrarFallo("a@ejemplo.com", t0);
        }
        assertTrue(intentos.bloqueado("a@ejemplo.com", t0));
        intentos.limpiar("a@ejemplo.com");
        assertFalse(intentos.bloqueado("a@ejemplo.com", t0));
    }
}
