package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.model.RolUsuario;
import com.cajapiura.preevaluacion.model.Usuario;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

/** Gestión de cuentas internas por el administrador, con protecciones para no quedarse sin acceso. */
@Service
public class AdminUsuarioService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Resultado de crear/restablecer: la contraseña temporal solo se conoce en este momento. */
    public static class ConPassword {
        private final Usuario usuario;
        private final String passwordTemporal;

        public ConPassword(Usuario usuario, String passwordTemporal) {
            this.usuario = usuario;
            this.passwordTemporal = passwordTemporal;
        }

        public Usuario getUsuario() { return usuario; }
        public String getPasswordTemporal() { return passwordTemporal; }
    }

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public AdminUsuarioService(UsuarioRepository repository, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.repository = repository;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    public List<Usuario> listar() {
        return repository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static boolean usernameValido(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }

    public static String generarPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    public ConPassword crear(String username, String nombreCompleto, RolUsuario rol, String actor) {
        String u = username == null ? "" : username.trim().toLowerCase();
        if (!usernameValido(u)) {
            throw new IllegalArgumentException("El usuario debe tener 3 a 30 caracteres: letras minúsculas, números, punto, guion o guion bajo");
        }
        if (nombreCompleto == null || nombreCompleto.isBlank() || nombreCompleto.trim().length() > 120) {
            throw new IllegalArgumentException("Ingresa el nombre completo (máximo 120 caracteres)");
        }
        if (rol == null) {
            throw new IllegalArgumentException("Selecciona un rol");
        }
        if (repository.findByUsernameIgnoreCase(u).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre");
        }
        String temporal = generarPassword();
        Usuario nuevo = repository.save(new Usuario(u, encoder.encode(temporal), nombreCompleto.trim(), rol));
        auditoria.registrar(actor, AccionAuditoria.USUARIO_CREADO, "usuario:" + u, "Rol " + rol.getEtiqueta());
        return new ConPassword(nuevo, temporal);
    }

    public Usuario cambiarActivo(Long id, boolean activo, String actor) {
        Usuario objetivo = obtener(id);
        if (!activo) {
            if (objetivo.getUsername().equalsIgnoreCase(actor)) {
                throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
            }
            if (objetivo.getRol() == RolUsuario.ADMIN && adminsActivosDistintosDe(objetivo) == 0) {
                throw new IllegalArgumentException("No se puede desactivar al último administrador activo");
            }
        }
        objetivo.setActivo(activo);
        Usuario guardado = repository.save(objetivo);
        auditoria.registrar(actor, activo ? AccionAuditoria.USUARIO_ACTIVADO : AccionAuditoria.USUARIO_DESACTIVADO,
                "usuario:" + objetivo.getUsername(), null);
        return guardado;
    }

    public Usuario cambiarRol(Long id, RolUsuario nuevoRol, String actor) {
        Usuario objetivo = obtener(id);
        if (nuevoRol == null) {
            throw new IllegalArgumentException("Selecciona un rol");
        }
        if (objetivo.getRol() == nuevoRol) {
            return objetivo;
        }
        if (objetivo.getRol() == RolUsuario.ADMIN) {
            if (objetivo.getUsername().equalsIgnoreCase(actor)) {
                throw new IllegalArgumentException("No puedes quitarte tu propio rol de administrador");
            }
            if (objetivo.isActivo() && adminsActivosDistintosDe(objetivo) == 0) {
                throw new IllegalArgumentException("No se puede degradar al último administrador activo");
            }
        }
        RolUsuario anterior = objetivo.getRol();
        objetivo.setRol(nuevoRol);
        Usuario guardado = repository.save(objetivo);
        auditoria.registrar(actor, AccionAuditoria.ROL_CAMBIADO, "usuario:" + objetivo.getUsername(),
                anterior.getEtiqueta() + " → " + nuevoRol.getEtiqueta());
        return guardado;
    }

    public ConPassword restablecerPassword(Long id, String actor) {
        Usuario objetivo = obtener(id);
        String temporal = generarPassword();
        objetivo.setPasswordHash(encoder.encode(temporal));
        Usuario guardado = repository.save(objetivo);
        auditoria.registrar(actor, AccionAuditoria.PASSWORD_RESTABLECIDA, "usuario:" + objetivo.getUsername(), null);
        return new ConPassword(guardado, temporal);
    }

    private Usuario obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private long adminsActivosDistintosDe(Usuario excluido) {
        return repository.findAll().stream()
                .filter(u -> u.getRol() == RolUsuario.ADMIN && u.isActivo() && !u.getId().equals(excluido.getId()))
                .count();
    }
}
