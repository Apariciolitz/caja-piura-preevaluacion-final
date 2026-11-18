package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.model.Cliente;
import com.cajapiura.preevaluacion.repository.ClienteRepository;
import com.cajapiura.preevaluacion.repository.SolicitudCreditoRepository;
import com.cajapiura.preevaluacion.model.SolicitudCredito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final SolicitudCreditoRepository solicitudRepository;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository repository, SolicitudCreditoRepository solicitudRepository,
                          PasswordEncoder encoder, AuditoriaService auditoria) {
        this.repository = repository;
        this.solicitudRepository = solicitudRepository;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    /**
     * Registra un cliente. Lanza IllegalArgumentException con un mensaje apto para mostrar al usuario.
     * El mensaje de correo repetido no revela detalles más allá de lo necesario.
     */
    public Cliente registrar(String nombre, String correo, String password, String confirmar,
                             boolean aceptaDatos, boolean aceptaTerminos) {
        List<String> errores = ClienteValidador.validarRegistro(nombre, correo, password, confirmar,
                aceptaDatos, aceptaTerminos);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errores));
        }
        String correoNormal = ClienteValidador.normalizarCorreo(correo);
        if (repository.existsByCorreoIgnoreCase(correoNormal)) {
            throw new IllegalArgumentException("No pudimos crear la cuenta con ese correo. Si ya te registraste, inicia sesión.");
        }
        Cliente cliente = new Cliente(correoNormal, encoder.encode(password), nombre.trim());
        cliente.setConsentimientoDatos(LocalDateTime.now());
        cliente.setVersionTerminos(ClienteValidador.VERSION_TERMINOS);
        try {
            cliente = repository.save(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("No pudimos crear la cuenta con ese correo. Si ya te registraste, inicia sesión.");
        }
        auditoria.registrar(correoNormal, AccionAuditoria.CLIENTE_REGISTRADO, "cliente:" + cliente.getId(),
                "Consentimiento de datos v" + ClienteValidador.VERSION_TERMINOS);
        return cliente;
    }

    public Optional<Cliente> buscarPorCorreo(String correo) {
        return repository.findByCorreoIgnoreCase(ClienteValidador.normalizarCorreo(correo));
    }

    public List<SolicitudCredito> solicitudesDe(Cliente cliente) {
        return solicitudRepository.findByClienteOrderByFechaCreacionDesc(cliente);
    }
}
