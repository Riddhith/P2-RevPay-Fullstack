package com.revature.revpay.config;

import com.revature.revpay.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LogManager.getLogger(SecurityConfig.class);

    private final JwtUtil jwtUtil;
    private final JwtAuthFilter jwtAuthFilter;
    private final String cookieName;

    public SecurityConfig(JwtUtil jwtUtil,
            JwtAuthFilter jwtAuthFilter,
            @Value("${jwt.cookie-name}") String cookieName) {
        this.jwtUtil = jwtUtil;
        this.jwtAuthFilter = jwtAuthFilter;
        this.cookieName = cookieName;
    }

    // ----------------------------------------------------------------
    // Password encoder
    // ----------------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ----------------------------------------------------------------
    // AuthenticationManager
    // ----------------------------------------------------------------

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ----------------------------------------------------------------
    // Security filter chain — stateless JWT
    // ----------------------------------------------------------------

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // No sessions — JWT handles authentication state
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL access rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/api/auth/**", "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/business/**").hasRole("BUSINESS")
                        .anyRequest().authenticated())

                // Form login — Spring Security handles credential check,
                // our success handler issues the JWT cookie
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("emailOrPhone")
                        .passwordParameter("password")
                        .successHandler(jwtLoginSuccessHandler())
                        .failureUrl("/auth/login?error=true")
                        .permitAll())

                // Logout — clear the JWT cookie (GET or POST)
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .addLogoutHandler((request, response, authentication) -> clearJwtCookie(response))
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .deleteCookies(cookieName)
                        .permitAll())

                // CSRF disabled — JWT provides authentication; CSRF not needed for JWT cookie
                // auth
                .csrf(csrf -> csrf.disable())

                // Place JWT filter before the standard auth filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ----------------------------------------------------------------
    // On successful form login: generate JWT and store in HttpOnly cookie
    // ----------------------------------------------------------------

    private AuthenticationSuccessHandler jwtLoginSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Derive role (strip "ROLE_" prefix)
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("PERSONAL");

            String token = jwtUtil.generateToken(userDetails.getUsername(), role);

            // Store JWT in HttpOnly cookie (not accessible via JavaScript)
            Cookie jwtCookie = new Cookie(cookieName, token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // set true in production (HTTPS)
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(-1); // -1 makes it a session cookie (expires on browser close)
            response.addCookie(jwtCookie);

            log.info("JWT issued for user '{}' with role '{}'", userDetails.getUsername(), role);
            
            if ("ADMIN".equals(role)) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/dashboard");
            }
        };
    }

    // ----------------------------------------------------------------
    // Helper: expire the JWT cookie immediately on logout
    // ----------------------------------------------------------------

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie expiredCookie = new Cookie(cookieName, "");
        expiredCookie.setHttpOnly(true);
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0); // delete immediately
        response.addCookie(expiredCookie);
    }
}
