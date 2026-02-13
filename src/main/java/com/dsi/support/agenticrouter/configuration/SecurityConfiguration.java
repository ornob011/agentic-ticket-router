package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.service.auth.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler;

import java.io.IOException;
import java.net.URI;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
        HttpSecurity http,
        DaoAuthenticationProvider daoAuthenticationProvider,
        AuthenticationEntryPoint apiAuthenticationEntryPoint,
        AccessDeniedHandler apiAccessDeniedHandler
    ) throws Exception {
        http
            .securityMatcher(AppRoutePolicy.PATTERN_API_SCOPE)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(AppRoutePolicy.publicApiEndpoints()).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiAuthenticationEntryPoint)
                .accessDeniedHandler(apiAccessDeniedHandler)
            )
            .authenticationProvider(daoAuthenticationProvider)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
            )
        ;

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(
        HttpSecurity http,
        DaoAuthenticationProvider daoAuthenticationProvider,
        AuthenticationEntryPoint appAuthenticationEntryPoint
    ) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(AppRoutePolicy.publicAppEndpoints()).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(appAuthenticationEntryPoint)
                .accessDeniedHandler(new HttpStatusAccessDeniedHandler(HttpStatus.FORBIDDEN))
            )
            .authenticationProvider(daoAuthenticationProvider)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
            )
        ;

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(customUserDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationEntryPoint appAuthenticationEntryPoint() {
        return (
            request,
            response,
            authException
        ) -> response.sendRedirect(AppRoutePolicy.PATH_LOGIN);
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (
            request,
            response,
            authException
        ) -> writeProblemDetail(
            request,
            response,
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Authentication required"
        );
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler() {
        return (
            request,
            response,
            accessDeniedException
        ) -> writeProblemDetail(
            request,
            response,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "Access denied"
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(
        @Value("${security.password.secret}") String secretKey
    ) {
        return new Pbkdf2PasswordEncoder(
            secretKey,
            128,
            1000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        );
    }

    private void writeProblemDetail(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String title,
        String detail
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        objectMapper.writeValue(
            response.getOutputStream(),
            problemDetail
        );
    }

}
