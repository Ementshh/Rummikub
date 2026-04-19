package com.rummikub.backend.controllers;

import com.rummikub.backend.models.User;
import com.rummikub.backend.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username dan password wajib diisi."));
            }
            User user = authService.register(username, password);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username dan password wajib diisi."));
            }
            Map<String, Object> result = authService.login(username, password);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
