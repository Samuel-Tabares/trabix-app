package com.trabix.equipment.config;

import com.trabix.equipment.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad para equipment-service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/error"
                ).permitAll()
                
                // === Stock (solo admin) ===
                .requestMatchers("/stock/**").hasRole("ADMIN")
                
                // === Asignaciones ===
                // Operaciones solo admin
                .requestMatchers(HttpMethod.POST, "/asignaciones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/asignaciones/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/asignaciones/*/devolver").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/asignaciones/*/cancelar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/asignaciones/*/confirmar-reposicion").hasRole("ADMIN")
                // Consultas admin
                .requestMatchers(HttpMethod.GET, "/asignaciones/estado/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/asignaciones/usuario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/asignaciones/resumen").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/asignaciones/pendientes-reposicion").hasRole("ADMIN")
                // Mis asignaciones - autenticados
                .requestMatchers("/asignaciones/me/**").authenticated()
                
                // === Mensualidades ===
                // Operaciones solo admin
                .requestMatchers(HttpMethod.POST, "/mensualidades/**").hasRole("ADMIN")
                // Consultas admin
                .requestMatchers(HttpMethod.GET, "/mensualidades/pendientes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/mensualidades/vencidos").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/mensualidades/pagados").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/mensualidades/asignacion/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/mensualidades/usuario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/mensualidades/mes/**").hasRole("ADMIN")
                // Mis pagos - autenticados
                .requestMatchers("/mensualidades/me/**").authenticated()
                
                // Resto requiere autenticación
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
