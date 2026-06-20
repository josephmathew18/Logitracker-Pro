package com.delivery.controller;

import com.delivery.model.Notification;
import com.delivery.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NotificationViewController {

    private final NotificationService notificationService;

    public NotificationViewController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String viewNotifications(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String username = authentication.getName();
        List<Notification> list = notificationService.getNotificationsForUser(username);
        model.addAttribute("notifications", list);
        return "notifications";
    }
}
