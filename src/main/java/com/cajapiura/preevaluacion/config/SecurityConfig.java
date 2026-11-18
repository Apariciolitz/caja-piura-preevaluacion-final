package com.cajapiura.preevaluacion.config;

import com.cajapiura.preevaluacion.model.AccionAuditoria;
import com.cajapiura.preevaluacion.repository.ClienteRepository;
import com.cajapiura.preevaluacion.service.AuditoriaService;
import com.cajapiura.preevaluacion.service.ClienteDetailsService;
import com.cajapiura.preevaluacion.service.ClienteValidador;
import com.cajapiura.preevaluacion.service.IntentosLogin;
import com.cajapiura.preevaluacion.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
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

    /**
     * Portal de CLIENTES: registro, inicio de sesión, simulador y pre-evaluación. Tiene su propio
     * administrador de autenticación (solo cuentas de clientes), separado del personal de Caja Piura.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain clienteFilterChain(HttpSecurity http, ClienteRepository clienteRepository,
                                                  PasswordEncoder passwordEncoder, AuditoriaService auditoria,
                                                  IntentosLogin intentos) throws Exception {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(new ClienteDetailsService(clienteRepository, intentos));
        proveedor.setPasswordEncoder(passwordEncoder);

        SavedRequestAwareAuthenticationSuccessHandler exito = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                intentos.limpiar(authentication.getName());
                auditoria.registrar(authentication.getName(), AccionAuditoria.LOGIN_CLIENTE, "sesión",
                        "IP " + ipCliente(request));
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
        exito.setDefaultTargetUrl("/cliente");

        http
            .securityMatcher("/cliente/**", "/simulador/**", "/pre-evaluacion/**")
            .authenticationManager(new ProviderManager(proveedor))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/cliente/login", "/cliente/registro").permitAll()
                .anyRequest().hasRole("CLIENTE")
            )
            .httpBasic(HttpBasicConfigurer::disable)
            .formLogin(form -> form
                .loginPage("/cliente/login")
                .loginProcessingUrl("/cliente/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .successHandler(exito)
                .failureHandler((request, response, exception) -> {
                    String correo = ClienteValidador.normalizarCorreo(request.getParameter("correo"));
                    boolean bloqueado = exception instanceof LockedException;
                    if (!bloqueado) {
                        intentos.registrarFallo(correo);
                    }
                    auditoria.registrar(correo, AccionAuditoria.LOGIN_CLIENTE_FALLIDO, "sesión",
                            (bloqueado ? "Cuenta bloqueada temporalmente. " : "") + "IP " + ipCliente(request));
                    response.sendRedirect(request.getContextPath() + "/cliente/login?" + (bloqueado ? "bloqueado" : "error"));
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/cliente/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Si el personal interno (asesor/admin) entra aquí, se le explica que esta zona es de clientes.
            .exceptionHandling(e -> e.accessDeniedHandler((request, response, ex) ->
                    response.sendRedirect(request.getContextPath() + "/cliente/login?acceso")));

        return http.build();
    }

    /**
     * Portal de COLABORADORES (asesores y administradores) y resto del sitio público.
     * Las cuentas internas solo las crea el administrador; no hay registro abierto.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, AuditoriaService auditoria,
                                           DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .authenticationManager(new ProviderManager(authenticationProvider))
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
