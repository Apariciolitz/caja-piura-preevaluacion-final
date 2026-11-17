package com.cajapiura.preevaluacion.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas para los documentos que adjunta el cliente (INC-08). Es una clase
 * sin dependencias de Spring para poder probarla de forma aislada.
 */
public final class ValidadorArchivos {

    public static final long TAMANIO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of(".pdf", ".jpg", ".jpeg", ".png");
    private static final Set<String> TIPOS = Set.of("application/pdf", "image/jpeg", "image/png");

    private ValidadorArchivos() {
    }

    /** Devuelve el mensaje de error, o vacío si el archivo es aceptable. */
    public static Optional<String> validar(String nombreOriginal, String contentType, long tamanioBytes) {
        if (tamanioBytes > TAMANIO_MAXIMO_BYTES) {
            return Optional.of("El archivo supera el máximo de 5 MB");
        }
        String extension = extensionDe(nombreOriginal);
        if (!EXTENSIONES.contains(extension)) {
            return Optional.of("Solo se aceptan archivos PDF, JPG o PNG");
        }
        if (contentType == null || !TIPOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            return Optional.of("El tipo de archivo no coincide con PDF, JPG o PNG");
        }
        return Optional.empty();
    }

    /** Extensión en minúsculas (con punto), o cadena vacía si no tiene. */
    public static String extensionDe(String nombre) {
        if (nombre == null || !nombre.contains(".")) {
            return "";
        }
        return nombre.substring(nombre.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    /** Quita rutas, comillas y caracteres de control del nombre original. */
    public static String sanearNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "documento";
        }
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        String limpio = soloNombre.replaceAll("[\\p{Cntrl}\"<>:*?|]", "_").trim();
        if (limpio.length() > 100) {
            limpio = limpio.substring(limpio.length() - 100);
        }
        return limpio.isEmpty() ? "documento" : limpio;
    }
}
