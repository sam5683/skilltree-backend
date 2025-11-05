package com.skilltree.skilltreebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration class for Spring Security settings.
 * Defines password encoding, user details, and HTTP security rules.
 * Supports Basic Auth initially, with plans for JWT integration.
 * Updated for Spring Security 6.x (Boot 3.x): Full lambda DSL, no deprecations.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Provides a BCryptPasswordEncoder bean for secure password hashing.
     * @return BCryptPasswordEncoder instance with default strength
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines in-memory user details for initial authentication.
     * @return UserDetailsService with an admin user (to be replaced with DB later)
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Exposes AuthenticationManager for manual auth (e.g., login endpoints).
     * @param config AuthenticationConfiguration
     * @return AuthenticationManager bean
     * @throws Exception if setup fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures the security filter chain using full Lambda DSL.
     * Allows public access to /api/users (sign-up), /api/users/register, /api/users/login, /api/users/check-email, /api/users/username/**, and /actuator/**.
     * Secures /api/users/admin/** with ADMIN role. Authenticated for others.
     * Stateless for APIs, with CORS. Prepares for JWT.
     * @param http HttpSecurity instance
     * @return SecurityFilterChain instance
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for stateless REST APIs
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS via global source
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless for JWT/Basic
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/users", "/api/users/register", "/api/users/login", "/api/users/check-email", "/api/users/username/**", "/actuator/**").permitAll() // Public endpoints + Actuator for dev (UPDATED: Added "/api/users" for sign-up POST)
                .requestMatchers("/api/users/admin/**").hasRole("ADMIN") // Secure admin operations only
                .anyRequest().authenticated() // Authenticated for all other /api/users/**
            )
            .httpBasic(basic -> basic.realmName("SkillTree")); // Temp Basic Auth (disable for JWT later)

        // Placeholder for JWT filter (uncomment/inject when ready)
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Global CORS configuration source (better than separate filter for Boot 3.x).
     * Allows requests from frontend (localhost:5500). Expand for prod domains.
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5500"); // Match your frontend
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("Authorization"); // For JWT tokens (frontend reads tokens)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Optional: If you still need a separate CorsFilter (legacy), but global source is preferred
    // @Bean
    // public CorsFilter corsFilter() {
    //     return new CorsFilter(corsConfigurationSource());
    // }
}