package com.authplatform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of("message", "Registered. Check your email for the verification link."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest req, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(req, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestBody Map<String, String> body) {
        authService.verifyEmail(body.get("token"));
        return ResponseEntity.ok(Map.of("message", "Email verified. You can now login."));
    }

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody AuthDtos.ForgotRequest req) {
        authService.forgotPassword(req.getEmail());
        // Identical response either way: blocks account enumeration
        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset link was sent."));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(@Valid @RequestBody AuthDtos.ResetRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset. All sessions were revoked."));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @Valid @RequestBody AuthDtos.UpdateProfileRequest req,
            org.springframework.security.core.Authentication auth) {
        if (!auth.getName().equalsIgnoreCase(req.getEmail())) {
            throw new RuntimeException("Forbidden: cannot edit another profile");
        }
        var user = authService.updateProfile(req.getEmail(), req.getFullName());
        return ResponseEntity.ok(Map.of("email", user.getEmail(), "fullName", user.getFullName()));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody AuthDtos.ChangePasswordRequest req,
            org.springframework.security.core.Authentication auth) {
        if (!auth.getName().equalsIgnoreCase(req.getEmail())) {
            throw new RuntimeException("Forbidden: cannot change another password");
        }
        authService.changePassword(req.getEmail(), req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated. All other sessions revoked."));
    }
}
