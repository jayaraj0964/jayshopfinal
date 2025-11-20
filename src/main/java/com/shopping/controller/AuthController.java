package com.shopping.controller;

import com.shopping.dto.LoginRequest;
import com.shopping.dto.RegisterRequest;
import com.shopping.dto.UserResponse;
import com.shopping.entity.User;
import com.shopping.repository.UserRepository;
import com.shopping.config.JwtAuthenticationFilter;
import com.shopping.config.JwtUtil;
import com.shopping.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);


@PostMapping("/login")
public ResponseEntity<String> login(@RequestBody LoginRequest request) {
    log.info("Login attempt for email: {}", request.getEmail());

    try {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Login successful for: {}", email);
        return ResponseEntity.ok(jwt);

    } catch (Exception e) {
        log.error("Login failed for {}: {}", request.getEmail(), e.getMessage());
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        User saved = authService.register(request);
        return ResponseEntity.ok(new UserResponse(saved.getId(), saved.getName(), saved.getEmail()));
    }
}