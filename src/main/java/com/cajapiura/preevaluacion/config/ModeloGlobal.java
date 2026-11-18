package com.cajapiura.preevaluacion.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expone a todas las vistas si la sesión actual es de administrador (para mostrar el acceso a /admin). */
@ControllerAdvice
public class ModeloGlobal {

    @ModelAttribute("esAdmin")
    public boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Verdadero si hay un cliente con sesión iniciada (para mostrar "Mi cuenta" en el menú público). */
    @ModelAttribute("esCliente")
    public boolean esCliente(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
    }
}
