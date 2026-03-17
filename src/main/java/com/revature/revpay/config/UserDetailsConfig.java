package com.revature.revpay.config;

import com.revature.revpay.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Extracted into its own config class to break the circular dependency:
 * SecurityConfig → JwtAuthFilter → UserDetailsService → SecurityConfig
 */
@Configuration
public class UserDetailsConfig {

    private final UserService userService;

    public UserDetailsConfig(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return emailOrPhone -> {
            com.revature.revpay.model.User u = userService.findByEmailOrPhone(emailOrPhone);
            if (u == null)
                throw new UsernameNotFoundException("User not found: " + emailOrPhone);
            String role = (u.getRole() != null && u.getRole().equals("ADMIN")) ? "ADMIN" : u.getAccountType();
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getEmail())
                    .password(u.getPasswordHash())
                    .roles(role) // PERSONAL, BUSINESS, or ADMIN
                    .build();
        };
    }
}
