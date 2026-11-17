package com.cajapiura.preevaluacion.config;

import com.cajapiura.preevaluacion.model.*;
import com.cajapiura.preevaluacion.repository.ComentarioSolicitudRepository;
import com.cajapiura.preevaluacion.repository.HistorialEstadoRepository;
import com.cajapiura.preevaluacion.repository.SolicitudCreditoRepository;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import com.cajapiura.preevaluacion.service.ParametroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDate;

/**
 * Carga usuarios y solicitudes de ejemplo en el arranque, solo si la base
 * de datos está vacía. Crea un pequeño "equipo" de asesores para poder
 * demostrar las funciones de colaboración (comentarios, asignación,
 * historial) desde la primera ejecución.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final SolicitudCreditoRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ComentarioSolicitudRepository comentarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParametroService parametroService;
    private final String passwordAdmin;
    private final String passwordAsesores;

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public DataSeeder(SolicitudCreditoRepository solicitudRepository,
                       UsuarioRepository usuarioRepository,
                       HistorialEstadoRepository historialRepository,
                       ComentarioSolicitudRepository comentarioRepository,
                       PasswordEncoder passwordEncoder,
                       ParametroService parametroService,
                       @Value("${app.seed.admin-password:}") String passwordAdmin,
                       @Value("${app.seed.asesor-password:}") String passwordAsesores) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
        this.comentarioRepository = comentarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.parametroService = parametroService;
        // INC-01: ya no hay contraseñas fijas en el código ni en el README. Se leen de variables
        // de entorno; si no están definidas, se genera una aleatoria y se muestra una sola vez en el log.
        this.passwordAdmin = definirPassword(passwordAdmin, "admin");
        this.passwordAsesores = definirPassword(passwordAsesores, "asesores (rpena, jtimana)");
    }

    private static String definirPassword(String configurada, String cuenta) {
        if (configurada != null && !configurada.isBlank()) {
            return configurada;
        }
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        String generada = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        log.warn("Contraseña generada para {}: {} (defina SEED_ADMIN_PASSWORD / SEED_ASESOR_PASSWORD para fijarla)",
                cuenta, generada);
        return generada;
    }

    @Override
    public void run(String... args) {
        parametroService.asegurarPorDefecto();
        if (usuarioRepository.count() == 0) {
            sembrarUsuarios();
        }
        if (solicitudRepository.count() == 0) {
            sembrarSolicitudes();
        }
    }

    private void sembrarUsuarios() {
        usuarioRepository.save(new Usuario("admin", passwordEncoder.encode(passwordAdmin),
                "Administrador del sistema", RolUsuario.ADMIN));
        usuarioRepository.save(new Usuario("rpena", passwordEncoder.encode(passwordAsesores),
                "Rosa Peña Castillo", RolUsuario.ASESOR));
        usuarioRepository.save(new Usuario("jtimana", passwordEncoder.encode(passwordAsesores),
                "Jorge Timaná Ruiz", RolUsuario.ASESOR));
    }

    private void sembrarSolicitudes() {
        Usuario rosa = usuarioRepository.findByUsernameIgnoreCase("rpena").orElse(null);
        Usuario jorge = usuarioRepository.findByUsernameIgnoreCase("jtimana").orElse(null);

        SolicitudCredito s1 = crear("María Fernanda Chunga Ruiz", "45678912", "987654321", "maria.chunga@example.com",
                new BigDecimal("1800"), new BigDecimal("5600"), "Capital de trabajo", EstadoSolicitud.CITA_AGENDADA, rosa);
        if (s1.getEstado() == EstadoSolicitud.CITA_AGENDADA) {
            s1.setCitaFecha(LocalDate.now().plusDays(3));
            s1.setCitaHora("10:30");
            s1.setCitaAgencia("Piura - Sede Central");
            solicitudRepository.save(s1);
        }
        comentarioRepository.save(new ComentarioSolicitud(s1, rosa,
                "Cliente con buen historial, ya la cité para el jueves."));

        crear("Jorge Luis Timaná Silva", "41234567", "912345678", "jorge.timana@example.com",
                new BigDecimal("1200"), new BigDecimal("7500"), "Compra de activos", EstadoSolicitud.PENDIENTE, jorge);

        SolicitudCredito s3 = crear("Rosa Elvira Peña More", "48765432", "998877665", "rosa.pena@example.com",
                new BigDecimal("2500"), new BigDecimal("6000"), "Vivienda", EstadoSolicitud.CONTACTADO, jorge);
        comentarioRepository.save(new ComentarioSolicitud(s3, jorge,
                "La contacté por WhatsApp, quedamos en que ella llama de vuelta esta semana."));

        crear("Luis Alberto Querevalú Nizama", "42345678", "945612378", "luis.querevalu@example.com",
                new BigDecimal("3200"), new BigDecimal("9000"), "Consumo personal", EstadoSolicitud.EN_REVISION, rosa);

        crear("Carmen Rosa Zapata Vilela", "47890123", "978123456", "carmen.zapata@example.com",
                new BigDecimal("1500"), new BigDecimal("4200"), "Educación", EstadoSolicitud.PENDIENTE, null);
    }

    private SolicitudCredito crear(String nombre, String dni, String telefono, String correo,
                                    BigDecimal ingreso, BigDecimal monto, String destino,
                                    EstadoSolicitud estado, Usuario asesor) {
        SolicitudCredito s = new SolicitudCredito();
        s.setNombreCompleto(nombre);
        s.setDni(dni);
        s.setTelefono(telefono);
        s.setCorreo(correo);
        s.setIngresoMensual(ingreso);
        s.setMontoSolicitado(monto);
        s.setDestinoCredito(destino);
        s.evaluar();
        s.setAsesorAsignado(asesor);
        SolicitudCredito guardada = solicitudRepository.save(s);

        historialRepository.save(new HistorialEstado(guardada, null, EstadoSolicitud.PENDIENTE, null,
                "Solicitud creada desde la pre-evaluación web"));

        if (estado != EstadoSolicitud.PENDIENTE) {
            guardada.setEstado(estado);
            guardada = solicitudRepository.save(guardada);
            historialRepository.save(new HistorialEstado(guardada, EstadoSolicitud.PENDIENTE, estado, asesor,
                    "Actualizado durante el seguimiento"));
        }

        return guardada;
    }
}
