package vn.edu.qnu.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.edu.qnu.events.audit.AuditService;
import vn.edu.qnu.events.auth.SessionJwtValidator;
import vn.edu.qnu.events.common.ApiError;
import vn.edu.qnu.events.common.ErrorCode;
import vn.edu.qnu.events.common.GlobalExceptionHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final Pattern PUBLISH_PATTERN = Pattern.compile("^/api/v1/events/([^/]+)/publish/?$");

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(requests -> {
                    // Public endpoints
                    requests.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll();
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();

                    if (docsEnabled) {
                        requests.requestMatchers(
                                "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**"
                        ).permitAll();
                    }

                    // Authenticated general endpoints
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated();
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated();
                    requests.requestMatchers("/api/v1/test/**", "/api/v1/test-*").authenticated();

                    // Event listing & viewing: STUDENT or EVENT_OWNER
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/{id}")
                            .hasAnyRole("STUDENT", "EVENT_OWNER");

                    // Event management: EVENT_OWNER
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/events").hasRole("EVENT_OWNER");
                    requests.requestMatchers(HttpMethod.PUT, "/api/v1/events/{id}").hasRole("EVENT_OWNER");
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/events/{id}/preview").hasRole("EVENT_OWNER");
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/events/{id}/publish").hasRole("EVENT_OWNER");
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/events/{id}/qr").hasRole("EVENT_OWNER");
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/classes").hasRole("EVENT_OWNER");

                    // Student registration & attendance: STUDENT
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/events/{id}/registrations").hasRole("STUDENT");
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/events/{id}/registrations/me").hasRole("STUDENT");
                    requests.requestMatchers(HttpMethod.POST, "/api/v1/events/{id}/attendance/qr").hasRole("STUDENT");
                    requests.requestMatchers(HttpMethod.GET, "/api/v1/me/history").hasRole("STUDENT");

                    // Deny all other requests
                    requests.anyRequest().denyAll();
                });

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.jwt-secret:}") String jwtSecret,
            ObjectProvider<SessionJwtValidator> sessionJwtValidatorProvider
    ) {
        String secret = (jwtSecret != null && jwtSecret.length() >= 32)
                ? jwtSecret
                : "qnu-events-dev-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!";
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        sessionJwtValidatorProvider.ifAvailable(validator -> {
            OAuth2TokenValidator<Jwt> withSession = new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    validator
            );
            decoder.setJwtValidator(withSession);
        });
        return decoder;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
        return (request, response, authException) -> {
            String traceId = GlobalExceptionHandler.resolveTraceId(request);
            ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
            if (authException != null) {
                String message = authException.getMessage();
                if (message != null && (message.contains("SESSION_REVOKED") || message.contains("session_revoked"))) {
                    errorCode = ErrorCode.SESSION_REVOKED;
                }
            }
            ApiError error = new ApiError(
                    errorCode.name(),
                    errorCode.getDefaultMessage(),
                    Map.of(),
                    traceId,
                    Instant.now()
            );
            writeJsonError(response, HttpStatus.UNAUTHORIZED, error, objectMapper);
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectProvider<ObjectMapper> objectMapperProvider, ObjectProvider<AuditService> auditServiceProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
        return (request, response, accessDeniedException) -> {
            String traceId = GlobalExceptionHandler.resolveTraceId(request);
            String path = request.getRequestURI();

            Matcher matcher = PUBLISH_PATTERN.matcher(path);
            if (matcher.matches()) {
                String eventId = matcher.group(1);
                auditServiceProvider.ifAvailable(auditService -> {
                    try {
                        UUID actorId = null;
                        String actorRole = "ANONYMOUS";
                        Authentication auth = (Authentication) request.getUserPrincipal();
                        if (auth != null) {
                            if (auth.getPrincipal() instanceof Jwt jwt) {
                                actorId = UUID.fromString(jwt.getSubject());
                            }
                            actorRole = auth.getAuthorities().stream()
                                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                                    .findFirst()
                                    .orElse("AUTHENTICATED");
                        }
                        auditService.record(
                                actorId,
                                actorRole,
                                "EVENT_PUBLISH_DENIED",
                                "EVENT",
                                eventId,
                                "DENIED",
                                "FORBIDDEN",
                                Map.of("path", path),
                                traceId
                        );
                    } catch (Exception ignored) {
                    }
                });
            }

            ApiError error = new ApiError(
                    ErrorCode.FORBIDDEN.name(),
                    ErrorCode.FORBIDDEN.getDefaultMessage(),
                    Map.of(),
                    traceId,
                    Instant.now()
            );
            writeJsonError(response, HttpStatus.FORBIDDEN, error, objectMapper);
        };
    }

    private static void writeJsonError(
            HttpServletResponse response,
            HttpStatus status,
            ApiError error,
            ObjectMapper objectMapper
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
