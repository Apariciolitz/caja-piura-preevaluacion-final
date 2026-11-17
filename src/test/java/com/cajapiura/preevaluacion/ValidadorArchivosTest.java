package com.cajapiura.preevaluacion;

import com.cajapiura.preevaluacion.service.ValidadorArchivos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** INC-08: los documentos adjuntos solo pueden ser PDF, JPG o PNG de hasta 5 MB. */
class ValidadorArchivosTest {

    @Test
    void aceptaPdfJpgYPng() {
        assertTrue(ValidadorArchivos.validar("dni.pdf", "application/pdf", 1000).isEmpty());
        assertTrue(ValidadorArchivos.validar("foto.JPG", "image/jpeg", 1000).isEmpty());
        assertTrue(ValidadorArchivos.validar("sustento.png", "image/png", 1000).isEmpty());
    }

    @Test
    void rechazaEjecutablesYPaginas() {
        assertTrue(ValidadorArchivos.validar("virus.exe", "application/octet-stream", 1000).isPresent());
        assertTrue(ValidadorArchivos.validar("pagina.html", "text/html", 1000).isPresent());
        assertTrue(ValidadorArchivos.validar("sin_extension", "application/pdf", 1000).isPresent());
    }

    @Test
    void rechazaExtensionQueNoCoincideConElTipo() {
        assertTrue(ValidadorArchivos.validar("falso.pdf", "text/html", 1000).isPresent());
    }

    @Test
    void rechazaArchivosMayoresA5Mb() {
        assertTrue(ValidadorArchivos.validar("grande.pdf", "application/pdf", 5L * 1024 * 1024 + 1).isPresent());
    }

    @Test
    void sanearNombreQuitaRutasYComillas() {
        assertEquals("a_b.pdf", ValidadorArchivos.sanearNombre("a\"b.pdf"));
        assertEquals("dni.pdf", ValidadorArchivos.sanearNombre("../../etc/dni.pdf"));
    }
}
