package com.cajapiura.preevaluacion.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Reglas de validación del registro de clientes. Lógica pura, sin dependencias, para poder probarla. */
public final class ClienteValidador {

    public static final String VERSION_TERMINOS = "2026-11";

    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern NOMBRE = Pattern.compile("^[\\p{L}][\\p{L} .'-]{2,119}$");
    private static final Set<String> COMUNES = Set.of(
            "12345678", "123456789", "1234567890", "password", "password1", "password123", "qwerty123",
            "contrasena", "contrasena1", "abc12345", "admin123", "cajapiura", "cajapiura1", "11111111");

    private ClienteValidador() {
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean correoValido(String correo) {
        String c = normalizarCorreo(correo);
        return c.length() <= 150 && CORREO.matcher(c).matches();
    }

    /** Requisitos: 8 a 72 caracteres, mayúscula, minúscula y número; no común ni derivada del correo. */
    public static List<String> erroresPassword(String password, String correo) {
        List<String> errores = new ArrayList<>();
        if (password == null || password.length() < 8) {
            errores.add("La contraseña debe tener al menos 8 caracteres.");
            return errores;
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            errores.add("La contraseña no puede superar 72 caracteres.");
        }
        if (!password.matches(".*\\p{Lu}.*")) errores.add("La contraseña debe incluir una letra mayúscula.");
        if (!password.matches(".*\\p{Ll}.*")) errores.add("La contraseña debe incluir una letra minúscula.");
        if (!password.matches(".*\\d.*")) errores.add("La contraseña debe incluir un número.");
        if (COMUNES.contains(password.toLowerCase(Locale.ROOT))) {
            errores.add("Esa contraseña es demasiado común. Elige otra.");
        }
        String local = normalizarCorreo(correo);
        int arroba = local.indexOf('@');
        if (arroba >= 4 && password.toLowerCase(Locale.ROOT).contains(local.substring(0, arroba))) {
            errores.add("La contraseña no debe contener tu correo.");
        }
        return errores;
    }

    public static List<String> validarRegistro(String nombre, String correo, String password, String confirmar,
                                               boolean aceptaDatos, boolean aceptaTerminos) {
        List<String> errores = new ArrayList<>();
        if (nombre == null || !NOMBRE.matcher(nombre.trim()).matches()) {
            errores.add("Ingresa tu nombre completo (solo letras, 3 a 120 caracteres).");
        }
        if (!correoValido(correo)) {
            errores.add("Ingresa un correo electrónico válido.");
        }
        errores.addAll(erroresPassword(password, correo));
        if (password != null && !password.equals(confirmar)) {
            errores.add("Las contraseñas no coinciden.");
        }
        if (!aceptaDatos) {
            errores.add("Debes autorizar el tratamiento de tus datos personales para registrarte.");
        }
        if (!aceptaTerminos) {
            errores.add("Debes aceptar los términos y condiciones.");
        }
        return errores;
    }
}
