package com.delivery.controller;

import com.delivery.model.Notification;
import com.delivery.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication == null) {
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
        long count = notificationService.getUnreadCount(authentication.getName());
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Notification> list = notificationService.getNotificationsForUser(authentication.getName());
        // Limit to 5
        List<Notification> recent = list.stream().limit(5).toList();
        return ResponseEntity.ok(recent);
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Integer id) {
        notificationService.markAsRead(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null) {
            notificationService.markAllAsRead(authentication.getName());
            response.put("success", true);
        } else {
            response.put("success", false);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable("id") Integer id) {
        notificationService.deleteNotification(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
