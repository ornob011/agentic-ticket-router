package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.filter.LoginPageFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final LoginPageFilter loginPageFilter;
    private final String rememberMeKey;
    private final int rememberMeTokenValiditySeconds;

    public SecurityConfiguration(
        LoginPageFilter loginPageFilter,
        @Value("${security.rememberme.key}") String rememberMeKey,
        @Value("${security.rememberme.token-validity-seconds}") int rememberMeTokenValiditySeconds
    ) {
        this.loginPageFilter = loginPageFilter;
        this.rememberMeKey = rememberMeKey;
        this.rememberMeTokenValiditySeconds = rememberMeTokenValiditySeconds;
    }

    @Bean
    public AuthenticationSuccessHandler postLoginRedirectHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/signup",
                    "/login",
                    "/js/**",
                    "/css/**",
                    "/images/**",
                    "/webjars/**",
                    "/logout",
                    "/error",
                    "/dev/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(loginPageFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .successHandler(postLoginRedirectHandler())
                .permitAll()
            )
            .rememberMe(rememberMe -> rememberMe
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(rememberMeTokenValiditySeconds)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            )
        ;

        return http.build();
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

}
