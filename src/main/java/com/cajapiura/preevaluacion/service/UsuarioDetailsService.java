package com.cajapiura.preevaluacion.service;

import com.cajapiura.preevaluacion.model.RolUsuario;
import com.cajapiura.preevaluacion.model.Usuario;
import com.cajapiura.preevaluacion.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga los usuarios internos (asesores/administradores) desde la base de
 * datos para la autenticación del panel, en lugar del usuario único en
 * memoria de la primera versión. Permite que varios asesores trabajen en
 * equipo, cada uno con su propia cuenta.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_ASESOR"));
        if (usuario.getRol() == RolUsuario.ADMIN) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .disabled(!usuario.isActivo())
                .authorities(autoridades)
                .build();
    }
}
