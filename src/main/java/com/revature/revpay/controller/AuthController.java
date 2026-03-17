package com.revature.revpay.controller;

import com.revature.revpay.service.UserService;
import com.revature.revpay.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LogManager.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final String cookieName;

    public AuthController(UserService userService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtUtil jwtUtil,
            @Value("${jwt.cookie-name}") String cookieName) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.cookieName = cookieName;
    }

    // ----------------------------------------------------------------
    // Thymeleaf pages
    // ----------------------------------------------------------------

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            Model model) {
        if (error != null)
            model.addAttribute("errorMsg", "Invalid email/phone or password.");
        if (logout != null)
            model.addAttribute("successMsg", "You have been logged out successfully.");
        if (expired != null)
            model.addAttribute("errorMsg", "Your session has expired. Please login again.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(defaultValue = "personal") String type, Model model) {
        model.addAttribute("accountType", type);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String accountType,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            // Business fields
            @RequestParam(required = false) String businessName,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String taxId,
            @RequestParam(required = false) String address,
            RedirectAttributes ra) {
        try {
            if (password == null || !password.equals(confirmPassword)) {
                ra.addFlashAttribute("errorMsg", "Passwords do not match");
                return "redirect:/auth/register?type=" + accountType;
            }
            if ("BUSINESS".equalsIgnoreCase(accountType)) {
                userService.registerBusiness(fullName, email, phone, username, password,
                        businessName, businessType, taxId, address);
            } else {
                userService.registerPersonal(fullName, email, phone, username, password);
            }
            ra.addFlashAttribute("successMsg", "Account created! Please login.");
            return "redirect:/auth/login";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            ra.addFlashAttribute("accountType", accountType);
            return "redirect:/auth/register?type=" + accountType;
        }
    }

    // ----------------------------------------------------------------
    // REST API login — returns JWT in JSON body AND sets cookie
    // Useful for Postman / mobile clients
    // ----------------------------------------------------------------

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody Map<String, String> body,
            HttpServletResponse response) {
        String emailOrPhone = body.get("emailOrPhone");
        String password = body.get("password");

        if (emailOrPhone == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "emailOrPhone and password are required"));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailOrPhone, password));

            UserDetails userDetails = userDetailsService.loadUserByUsername(emailOrPhone);
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("PERSONAL");

            String token = jwtUtil.generateToken(userDetails.getUsername(), role);

            // Also set the cookie so browser clients are authenticated
            Cookie jwtCookie = new Cookie(cookieName, token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400);
            response.addCookie(jwtCookie);

            log.info("REST login successful for '{}'", emailOrPhone);
            return ResponseEntity.ok(Map.of("token", token, "role", role));

        } catch (AuthenticationException e) {
            log.warn("REST login failed for '{}': {}", emailOrPhone, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }
}
