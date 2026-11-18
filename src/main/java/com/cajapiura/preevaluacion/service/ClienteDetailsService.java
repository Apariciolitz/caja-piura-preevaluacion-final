package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.Cliente;
import com.cajapiura.preevaluacion.repository.ClienteRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Autenticación de clientes (por correo). No es un bean a propósito: así no se mezcla con el
 * {@link UsuarioDetailsService} del personal interno y cada portal valida solo a sus propias cuentas.
 */
public class ClienteDetailsService implements UserDetailsService {

    private final ClienteRepository repository;
    private final IntentosLogin intentos;

    public ClienteDetailsService(ClienteRepository repository, IntentosLogin intentos) {
        this.repository = repository;
        this.intentos = intentos;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Cliente cliente = repository.findByCorreoIgnoreCase(ClienteValidador.normalizarCorreo(correo))
                .orElseThrow(() -> new UsernameNotFoundException("Cliente no encontrado"));
        return User.withUsername(cliente.getCorreo())
                .password(cliente.getPasswordHash())
                .disabled(!cliente.isActivo())
                .accountLocked(intentos.bloqueado(cliente.getCorreo()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                .build();
    }
}
