package com.scaler.price.core.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())  // Disable CSRF for file upload
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI endpoints
                        .requestMatchers("/api/v1/swagger-ui/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/swagger-resources/**", "/swagger-resources/**").permitAll()
                        .requestMatchers("/api/v1/webjars/**", "/webjars/**").permitAll()
                        // API endpoints
                        .requestMatchers("/api/v1/prices/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic();  // Enable HTTP Basic authentication

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}