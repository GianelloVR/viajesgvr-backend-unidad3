package com.example.viajesgvr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        // Permite las peticiones preflight del navegador
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints publicos para que clientes puedan ver paquetes
                        .requestMatchers(HttpMethod.GET, "/api/tour-packages/**").permitAll()

                        // Reportes solo para ADMIN
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")

                        // Gestion admin de paquetes
                        .requestMatchers(HttpMethod.POST, "/api/tour-packages/").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tour-packages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tour-packages/**").hasRole("ADMIN")

                        // Usuario autenticado puede consultar su propio perfil sincronizado con Keycloak
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()

                        // Gestion admin general
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")

                        // Reservas y pagos requieren usuario autenticado
                        .requestMatchers("/api/reservations/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/payments/").authenticated()

                        // Cualquier otra ruta requiere autenticacion
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOriginPatterns(List.of(
                    "http://localhost:*",
                    "https://localhost:*",
                    "http://18.216.240.192:*",
                    "https://18.216.240.192:*"
            ));

            config.setAllowedMethods(List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
            ));

            config.setAllowedHeaders(List.of(
                    "Authorization",
                    "Content-Type",
                    "Accept",
                    "Origin"
            ));

            config.setExposedHeaders(List.of(
                    "Authorization"
            ));

            config.setAllowCredentials(true);

            return config;
        };
    }

    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Object rolesObject = realmAccess.get("roles");

                if (rolesObject instanceof List<?> roles) {
                    roles.forEach(role -> addRole(authorities, role));
                }
            }

            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

            if (resourceAccess != null) {
                Object frontendClient = resourceAccess.get("viajesgvr-frontend");

                if (frontendClient instanceof Map<?, ?> clientMap) {
                    Object clientRolesObject = clientMap.get("roles");

                    if (clientRolesObject instanceof List<?> clientRoles) {
                        clientRoles.forEach(role -> addRole(authorities, role));
                    }
                }
            }

            return new JwtAuthenticationToken(jwt, authorities);
        };
    }

    private void addRole(Collection<GrantedAuthority> authorities, Object roleObject) {
        if (roleObject == null) {
            return;
        }

        String role = roleObject.toString();

        if (role.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority(role));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
    }
}