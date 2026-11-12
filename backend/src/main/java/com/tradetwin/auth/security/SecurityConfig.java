package com.tradetwin.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/auth/**",
                                "/api/health/**",
                                "/api/v1/health/**",
                                "/actuator/health/**",
                                "/api/payments/paypal/webhook",
                                "/api/v1/payments/paypal/webhook",
                                "/error"
                        ).permitAll()
                        .requestMatchers(
                                "/api/catalog/seller/**",
                                "/api/v1/catalog/seller/**",
                                "/api/sellers/**",
                                "/api/v1/sellers/**",
                                "/api/v1/seller/**",
                                "/api/orders/seller/**",
                                "/api/v1/orders/seller/**"
                        ).hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**", "/api/v1/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/search", "/api/v1/catalog/search").permitAll()
                        .requestMatchers(
                                "/api/intents/**",
                                "/api/v1/intents/**",
                                "/api/intent/**",
                                "/api/v1/intent/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/negotiations/buyer/**",
                                "/api/v1/negotiations/buyer/**"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/negotiations/*/stream",
                                "/api/v1/negotiations/*/stream",
                                "/api/negotiations/*/events",
                                "/api/v1/negotiations/*/events",
                                "/api/negotiations/*/compare-offers",
                                "/api/v1/negotiations/*/compare-offers"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/negotiations/*", "/api/v1/negotiations/*").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
