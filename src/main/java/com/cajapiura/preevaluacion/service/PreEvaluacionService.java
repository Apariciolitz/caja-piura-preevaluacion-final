package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.*;
import com.cajapiura.preevaluacion.repository.ComentarioSolicitudRepository;
import com.cajapiura.preevaluacion.repository.HistorialEstadoRepository;
import com.cajapiura.preevaluacion.repository.SolicitudCreditoRepository;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Contiene la regla de negocio central del proyecto (pre-evaluación
 * automática) y coordina el resto de funcionalidades: asignación de
 * asesor, historial de estados y comentarios colaborativos.
 */
@Service
public class PreEvaluacionService {

    private final SolicitudCreditoRepository repository;
    private final HistorialEstadoRepository historialRepository;
    private final ComentarioSolicitudRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PreEvaluacionService(SolicitudCreditoRepository repository,
                                 HistorialEstadoRepository historialRepository,
                                 ComentarioSolicitudRepository comentarioRepository,
                                 UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.historialRepository = historialRepository;
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Evalua y persiste una nueva solicitud. Asigna automáticamente al asesor con menos casos activos. */
    public SolicitudCredito evaluarYGuardar(SolicitudCredito solicitud) {
        solicitud.evaluar();
        solicitud.setAsesorAsignado(asesorConMenosCargaActual());
        SolicitudCredito guardada = repository.save(solicitud);

        HistorialEstado inicio = new HistorialEstado(guardada, null, guardada.getEstado(),
                null, "Solicitud creada desde la pre-evaluación web");
        historialRepository.save(inicio);

        return guardada;
    }

    /** Reparte la carga de trabajo: asigna al asesor activo con menos solicitudes pendientes. */
    private Usuario asesorConMenosCargaActual() {
        List<Usuario> asesores = usuarioRepository.findByActivoTrueOrderByNombreCompletoAsc();
        if (asesores.isEmpty()) {
            return null;
        }
        return asesores.stream()
                .min(Comparator.comparingLong(a ->
                        repository.countByAsesorAsignadoAndEstadoNot(a, EstadoSolicitud.CERRADO)))
                .orElse(asesores.get(0));
    }

    public Optional<SolicitudCredito> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** Búsqueda para las páginas públicas del cliente: por código no adivinable (INC-02). */
    public Optional<SolicitudCredito> buscarPorCodigo(String codigoPublico) {
        return repository.findByCodigoPublico(codigoPublico);
    }

    public List<SolicitudCredito> listarTodas() {
        return repository.findAllByOrderByFechaCreacionDesc();
    }

    /** Lista filtrada para el panel del asesor (búsqueda y filtros). */
    public List<SolicitudCredito> buscarConFiltros(String texto, EstadoSolicitud estado,
                                                     ResultadoEvaluacion resultado, Long asesorId) {
        String textoNormalizado = texto == null ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(s -> textoNormalizado.isEmpty()
                        || s.getNombreCompleto().toLowerCase().contains(textoNormalizado)
                        || s.getDni().contains(textoNormalizado))
                .filter(s -> estado == null || s.getEstado() == estado)
                .filter(s -> resultado == null || s.getResultado() == resultado)
                .filter(s -> asesorId == null || (s.getAsesorAsignado() != null && asesorId.equals(s.getAsesorAsignado().getId())))
                .toList();
    }

    public long contarPorResultado(ResultadoEvaluacion resultado) {
        return repository.countByResultado(resultado);
    }

    public long contarPorEstado(EstadoSolicitud estado) {
        return repository.countByEstado(estado);
    }

    public SolicitudCredito agendarCita(Long id, LocalDate fecha, String hora, String agencia) {
        if (fecha == null || !fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la cita debe ser posterior a hoy");
        }
        SolicitudCredito solicitud = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        solicitud.setCitaFecha(fecha);
        solicitud.setCitaHora(hora);
        solicitud.setCitaAgencia(agencia);
        registrarCambioEstado(solicitud, EstadoSolicitud.CITA_AGENDADA, null, "Cita agendada por el cliente");
        return repository.save(solicitud);
    }

    public SolicitudCredito actualizarEstado(Long id, EstadoSolicitud nuevoEstado, String notas, Usuario usuario) {
        SolicitudCredito solicitud = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (notas != null && !notas.isBlank()) {
            solicitud.setNotasAsesor(notas);
        }
        registrarCambioEstado(solicitud, nuevoEstado, usuario, null);
        return repository.save(solicitud);
    }

    /** Reasigna el caso, validando al nuevo asesor y dejando registro en el historial (INC-06). */
    public SolicitudCredito reasignarAsesor(Long id, Long nuevoAsesorId, Usuario quienReasigna) {
        SolicitudCredito solicitud = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        Usuario nuevoAsesor = usuarioRepository.findById(nuevoAsesorId)
                .orElseThrow(() -> new IllegalArgumentException("Asesor no encontrado"));
        if (!nuevoAsesor.isActivo() || nuevoAsesor.getRol() != RolUsuario.ASESOR) {
            throw new IllegalArgumentException("Solo se puede asignar a un asesor activo");
        }
        Usuario anterior = solicitud.getAsesorAsignado();
        solicitud.setAsesorAsignado(nuevoAsesor);
        SolicitudCredito guardada = repository.save(solicitud);

        String detalle = "Reasignado de "
                + (anterior == null ? "sin asesor" : anterior.getNombreCompleto())
                + " a " + nuevoAsesor.getNombreCompleto();
        historialRepository.save(new HistorialEstado(guardada, guardada.getEstado(), guardada.getEstado(),
                quienReasigna, detalle));
        return guardada;
    }

    private void registrarCambioEstado(SolicitudCredito solicitud, EstadoSolicitud nuevoEstado,
                                        Usuario usuario, String detalle) {
        EstadoSolicitud anterior = solicitud.getEstado();
        solicitud.setEstado(nuevoEstado);
        HistorialEstado registro = new HistorialEstado(solicitud, anterior, nuevoEstado, usuario, detalle);
        historialRepository.save(registro);
    }

    public List<HistorialEstado> historialDe(SolicitudCredito solicitud) {
        return historialRepository.findBySolicitudOrderByFechaAsc(solicitud);
    }

    public List<ComentarioSolicitud> comentariosDe(SolicitudCredito solicitud) {
        return comentarioRepository.findBySolicitudOrderByFechaAsc(solicitud);
    }

    public ComentarioSolicitud agregarComentario(Long solicitudId, Usuario autor, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        String limpio = texto.trim();
        if (limpio.length() > 1000) {
            throw new IllegalArgumentException("El comentario no puede superar los 1000 caracteres");
        }
        SolicitudCredito solicitud = repository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        ComentarioSolicitud comentario = new ComentarioSolicitud(solicitud, autor, limpio);
        return comentarioRepository.save(comentario);
    }
}
