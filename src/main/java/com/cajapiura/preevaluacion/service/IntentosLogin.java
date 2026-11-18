package com.cajapiura.preevaluacion.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo temporal tras demasiados intentos fallidos de inicio de sesión (protección contra fuerza bruta).
 * Es en memoria: se reinicia al reiniciar la aplicación, lo que es suficiente para esta etapa.
 */
@Component
public class IntentosLogin {

    public static final int MAX_FALLOS = 5;
    public static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    private static String clave(String correo) {
        return ClienteValidador.normalizarCorreo(correo);
    }

    public void registrarFallo(String correo, Instant ahora) {
        if (clave(correo).isEmpty()) return;
        Deque<Instant> cola = fallos.computeIfAbsent(clave(correo), k -> new ArrayDeque<>());
        synchronized (cola) {
            depurar(cola, ahora);
            cola.addLast(ahora);
        }
    }

    public boolean bloqueado(String correo, Instant ahora) {
        Deque<Instant> cola = fallos.get(clave(correo));
        if (cola == null) return false;
        synchronized (cola) {
            depurar(cola, ahora);
            return cola.size() >= MAX_FALLOS;
        }
    }

    public void limpiar(String correo) {
        fallos.remove(clave(correo));
    }

    private static void depurar(Deque<Instant> cola, Instant ahora) {
        while (!cola.isEmpty() && cola.peekFirst().isBefore(ahora.minus(VENTANA))) {
            cola.removeFirst();
        }
    }

    public void registrarFallo(String correo) { registrarFallo(correo, Instant.now()); }
    public boolean bloqueado(String correo) { return bloqueado(correo, Instant.now()); }
}
