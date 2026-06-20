package com.delivery.controller;

import com.delivery.model.Notification;
import com.delivery.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class NotificationViewController {

    private final NotificationService notificationService;

    public NotificationViewController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String viewNotifications(Authentication authentication,
                                    @RequestParam(value = "search", required = false) String search,
                                    @RequestParam(value = "type", required = false) String type,
                                    @RequestParam(value = "priority", required = false) String priority,
                                    Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String username = authentication.getName();
        List<Notification> list = notificationService.searchAndFilterNotifications(username, search, type, priority);
        model.addAttribute("notifications", list);
        model.addAttribute("search", search);
        model.addAttribute("typeFilter", type);
        model.addAttribute("priorityFilter", priority);
        return "notifications";
    }
}
