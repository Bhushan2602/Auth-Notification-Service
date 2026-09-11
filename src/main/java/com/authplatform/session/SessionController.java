package com.authplatform.session;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> sessions(Authentication auth) {
        List<Map<String, Object>> out = sessionService.sessions(auth.getName()).stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "device", t.getDeviceLabel(),
                        "expiresAt", t.getExpiresAt().toString()))
                .toList();
        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> revokeOne(@PathVariable Long id, Authentication auth) {
        sessionService.revokeOne(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> revokeAll(Authentication auth) {
        sessionService.revokeAll(auth.getName());
        return ResponseEntity.ok(Map.of("message", "All sessions revoked. Login again."));
    }
}
