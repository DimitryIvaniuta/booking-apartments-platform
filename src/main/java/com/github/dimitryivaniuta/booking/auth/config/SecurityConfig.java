package com.github.dimitryivaniuta.booking.auth.config;

import com.github.dimitryivaniuta.booking.auth.security.RevokedTokenFilter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * <p>Access tokens are server-signed JWTs (RS256). The API validates them as a resource server.</p>
 *
 * <p>Expected JWT claims:
 * <ul>
 *   <li>sub: userId (UUID)</li>
 *   <li>email: user email</li>
 *   <li>roles: ["USER","ADMIN"]</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RevokedTokenFilter revokedTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 12 is a solid default in 2026; increase if your infra allows it.
        return new BCryptPasswordEncoder(12);
    }

    /**
     * JWT encoder for issuing RS256 access tokens.
     * Uses Spring's PEM converters (no manual Base64 parsing).
     */
    @Bean
    public JwtEncoder jwtEncoder(
            @Value("classpath:keys/jwt-private.pem") Resource privatePem,
            @Value("classpath:keys/jwt-public.pem") Resource publicPem
    ) throws Exception {
        RSAPublicKey publicKey =
                (RSAPublicKey) RsaKeyConverters.x509().convert(publicPem.getInputStream());
        RSAPrivateKey privateKey =
                (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(privatePem.getInputStream());

        RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    /**
     * JWT decoder for verifying RS256 access tokens.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("classpath:keys/jwt-public.pem") Resource publicPem) throws Exception {
        RSAPublicKey publicKey =
                (RSAPublicKey) RsaKeyConverters.x509().convert(publicPem.getInputStream());
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // infra
                        .requestMatchers("/actuator/**").permitAll()

                        // auth API
                        .requestMatchers("/api/auth/**").permitAll()

                        // public reads
                        .requestMatchers(HttpMethod.GET, "/api/availability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/apartments/**").permitAll()

                        // admin writes
                        .requestMatchers(HttpMethod.POST, "/api/apartments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/apartments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/apartments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/apartments/**").hasRole("ADMIN")

                        // bookings require auth
                        .requestMatchers("/api/bookings/**").authenticated()

                        // everything else
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                );

        // Enforce access-token revocation AFTER Bearer token is authenticated into SecurityContext
        http.addFilterAfter(revokedTokenFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object raw = jwt.getClaims().get("roles");
            if (raw instanceof List<?> list) {
                return list.stream()
                        .filter(v -> v != null)
                        .map(v -> "ROLE_" + v.toString())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        });
        return c;
    }
}
