package com.cajapiura.preevaluacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

/** Fija el locale de la aplicación en español (Perú) para todas las vistas. */
@Configuration
public class WebConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(new Locale("es", "PE"));
    }
}
