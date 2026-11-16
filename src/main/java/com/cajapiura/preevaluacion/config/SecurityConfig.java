package com.cajapiura.preevaluacion.config;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/asesor/**").hasRole("ASESOR")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(HttpBasicConfigurer::disable)
            .formLogin(form -> form
                .loginPage("/asesor/login")
                .loginProcessingUrl("/asesor/login")
                .defaultSuccessUrl("/asesor", true)
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
}
