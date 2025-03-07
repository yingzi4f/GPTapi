package com.airepair.controller;

import com.airepair.dto.LoginRequest;
import com.airepair.dto.LoginResponse;
import com.airepair.dto.RegisterRequest;
import com.airepair.entity.User;
import com.airepair.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        try {
            log.info("User trying to register: {}", request.getUsername());
            return ResponseEntity.ok(authService.register(request));
        } catch (Exception e) {
            log.error("Registration failed for user: " + request.getUsername(), e);
            return ResponseEntity.badRequest().body((User) null);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            log.info("User trying to login: {}", request.getUsername());
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            log.error("Login failed for user: " + request.getUsername(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}
