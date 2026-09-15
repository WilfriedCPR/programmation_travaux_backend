package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.NotificationDTO;
import com.gescli.ProgrammationTravaux.dto.PushSubscriptionRequestDTO;
import com.gescli.ProgrammationTravaux.service.NotificationService;
import com.gescli.ProgrammationTravaux.service.VapidPushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final VapidPushService pushService;

    @GetMapping
    public List<NotificationDTO> list(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.list(jwt.getSubject());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", notificationService.unreadCount(jwt.getSubject()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> read(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        notificationService.markRead(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vapid-public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", pushService.getPublicKey());
    }

    @PostMapping("/push-subscriptions")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PushSubscriptionRequestDTO req) {
        pushService.subscribe(jwt.getSubject(), req.getEndpoint(), req.getP256dh(), req.getAuth());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push-subscriptions")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal Jwt jwt, @RequestParam String endpoint) {
        pushService.unsubscribe(jwt.getSubject(), endpoint);
        return ResponseEntity.noContent().build();
    }
}
