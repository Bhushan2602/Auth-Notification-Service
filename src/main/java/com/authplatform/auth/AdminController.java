package com.authplatform.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> users() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "fullName", u.getFullName(),
                        "role", u.getRole(),
                        "verified", u.isEmailVerified(),
                        "provider", u.getProvider()))
                .toList());
    }

    @PutMapping("/role")
    public ResponseEntity<Map<String, String>> setRole(@RequestBody AuthDtos.RoleRequest req) {
        var user = authService.setRole(req.getEmail(), req.getRole());
        return ResponseEntity.ok(Map.of("email", user.getEmail(), "role", user.getRole()));
    }
}
