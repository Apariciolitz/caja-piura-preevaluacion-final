package com.cajapiura.preevaluacion.config;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.service.AuditoriaService;
import com.cajapiura.preevaluacion.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Solo el panel del asesor (/asesor/**) exige autenticación. La landing,
 * el formulario de pre-evaluación, el simulador de crédito y el
 * agendamiento de citas son públicos.
 *
 * La autenticación ahora está respaldada por base de datos
 * (ver UsuarioDetailsService y DataSeeder), para soportar varios asesores
 * trabajando en equipo con sus propias cuentas.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UsuarioDetailsService usuarioDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuditoriaService auditoria) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/asesor/**").hasRole("ASESOR")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(HttpBasicConfigurer::disable)
            .formLogin(form -> form
                .loginPage("/asesor/login")
                .loginProcessingUrl("/asesor/login")
                // El administrador entra a su consola; el asesor, a su panel. Ambos quedan en la bitácora.
                .successHandler((request, response, authentication) -> {
                    auditoria.registrar(authentication.getName(), AccionAuditoria.LOGIN_OK, "sesión",
                            "IP " + ipCliente(request));
                    boolean admin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/asesor"));
                })
                .failureHandler((request, response, exception) -> {
                    String intento = request.getParameter("username");
                    auditoria.registrar(intento, AccionAuditoria.LOGIN_FALLIDO, "sesión",
                            "IP " + ipCliente(request));
                    response.sendRedirect(request.getContextPath() + "/asesor/login?error");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/asesor/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /** Detrás del proxy de Render la IP real llega en X-Forwarded-For. */
    private static String ipCliente(jakarta.servlet.http.HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
