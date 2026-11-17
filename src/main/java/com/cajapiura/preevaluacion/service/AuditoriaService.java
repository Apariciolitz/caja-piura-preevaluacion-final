package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.model.RegistroAuditoria;
import com.cajapiura.preevaluacion.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Registra y consulta la bitácora de auditoría. Un fallo al auditar nunca debe romper la operación principal. */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final RegistroAuditoriaRepository repository;

    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String usuario, AccionAuditoria accion, String entidad, String detalle) {
        try {
            repository.save(new RegistroAuditoria(
                    recortar(usuario == null || usuario.isBlank() ? "sistema" : usuario, 60),
                    accion, recortar(entidad, 80), recortar(detalle, 400)));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el evento de auditoría {}: {}", accion, e.getMessage());
        }
    }

    /** Eventos recientes filtrados por usuario (contiene) y/o tipo de acción. */
    public List<RegistroAuditoria> buscar(String usuario, AccionAuditoria accion) {
        String u = usuario == null ? "" : usuario.trim().toLowerCase();
        return repository.findTop500ByOrderByFechaDesc().stream()
                .filter(r -> u.isEmpty() || (r.getUsuario() != null && r.getUsuario().toLowerCase().contains(u)))
                .filter(r -> accion == null || r.getAccion() == accion)
                .toList();
    }

    public static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replaceAll("[\\r\\n\\t]+", " ").trim();
        return limpio.length() <= max ? limpio : limpio.substring(0, max - 1) + "…";
    }
}
