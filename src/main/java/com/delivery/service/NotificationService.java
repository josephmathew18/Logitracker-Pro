package com.delivery.service;

import com.delivery.model.Notification;
import com.delivery.model.User;
import com.delivery.repository.NotificationRepository;
import com.delivery.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(User recipient, String title, String message, String type) {
        Notification notification = new Notification(recipient, title, message, type);
        Notification saved = notificationRepository.save(notification);

        // Simulated SMS and Email Channels
        System.out.println("======================================================================");
        System.out.println("[EMAIL NOTIFICATION SENDER MOCK]");
        System.out.println("To: " + recipient.getUsername() + "@logitrackpro.com");
        System.out.println("Subject: " + title);
        System.out.println("Message: " + message);
        System.out.println("----------------------------------------------------------------------");
        System.out.println("[SMS NOTIFICATION SENDER MOCK]");
        System.out.println("To Agent/User: " + recipient.getUsername());
        System.out.println("SMS Text: " + title + " - " + message);
        System.out.println("======================================================================");

        return saved;
    }

    @Transactional
    public Notification createNotification(String username, String title, String message, String type) {
        User recipient = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return createNotification(recipient, title, message, type);
    }

    public List<Notification> getNotificationsForUser(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    public List<Notification> getUnreadNotificationsForUser(String username) {
        return notificationRepository.findByRecipientUsernameAndIsReadOrderByCreatedAtDesc(username, false);
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndIsRead(username, false);
    }

    @Transactional
    public void markAsRead(Integer id) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification notification = opt.get();
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> unread = getUnreadNotificationsForUser(username);
        for (Notification n : unread) {
            n.setRead(true);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public Notification createNotification(User recipient, String role, String title, String message, String type, String priority) {
        Notification notification = new Notification(recipient, role, title, message, type, priority);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Integer id) {
        notificationRepository.deleteById(id);
    }

    public List<Notification> searchAndFilterNotifications(String username, String search, String typeFilter, String priorityFilter) {
        List<Notification> list = getNotificationsForUser(username);
        
        return list.stream()
                .filter(n -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String term = search.toLowerCase();
                    return n.getTitle().toLowerCase().contains(term) || n.getMessage().toLowerCase().contains(term);
                })
                .filter(n -> {
                    if (typeFilter == null || typeFilter.trim().isEmpty()) return true;
                    if ("unread".equalsIgnoreCase(typeFilter)) {
                        return !n.isRead();
                    }
                    return n.getType().equalsIgnoreCase(typeFilter);
                })
                .filter(n -> {
                    if (priorityFilter == null || priorityFilter.trim().isEmpty()) return true;
                    return n.getPriority().equalsIgnoreCase(priorityFilter);
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
