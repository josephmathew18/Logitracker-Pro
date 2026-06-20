package com.delivery.event;

import com.delivery.model.Agent;
import com.delivery.model.Customer;
import com.delivery.model.User;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.CustomerRepository;
import com.delivery.repository.UserRepository;
import com.delivery.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final CustomerRepository customerRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public NotificationEventListener(NotificationService notificationService,
                                     UserRepository userRepository,
                                     AgentRepository agentRepository,
                                     CustomerRepository customerRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.customerRepository = customerRepository;
    }

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        logger.info("Processing NotificationEvent for recipient: {}, title: {}", event.getRecipientUsername(), event.getTitle());

        Optional<User> recipientOpt = userRepository.findByUsername(event.getRecipientUsername());
        if (recipientOpt.isEmpty()) {
            logger.warn("Recipient user not found: {}", event.getRecipientUsername());
            return;
        }

        User recipient = recipientOpt.get();

        // 1. Create In-App Notification database entry for the target recipient
        notificationService.createNotification(recipient, recipient.getRole(), event.getTitle(), event.getMessage(), event.getType(), event.getPriority());

        // 2. Role-Based Routing: If recipient is not Admin, duplicate notification to Admin
        if (!"ADMIN".equalsIgnoreCase(recipient.getRole())) {
            Optional<User> adminOpt = userRepository.findByUsername("admin");
            if (adminOpt.isPresent()) {
                User admin = adminOpt.get();
                String adminTitle = "[" + recipient.getRole() + " Alert] " + event.getTitle();
                notificationService.createNotification(admin, "ADMIN", adminTitle, event.getMessage(), event.getType(), event.getPriority());
            }
        }

        // 3. Resolve recipient email and phone details
        String email = null;
        String phone = null;

        if ("AGENT".equalsIgnoreCase(recipient.getRole())) {
            Optional<Agent> agent = agentRepository.findByUserUsername(recipient.getUsername());
            if (agent.isPresent()) {
                email = agent.get().getEmail();
                phone = agent.get().getPhone();
            }
        } else if ("CUSTOMER".equalsIgnoreCase(recipient.getRole())) {
            Optional<Customer> customer = customerRepository.findByUserUsername(recipient.getUsername());
            if (customer.isPresent()) {
                email = customer.get().getEmail();
                phone = customer.get().getPhone();
            }
        }

        if (email == null) {
            email = recipient.getUsername() + "@logitrackpro.com";
        }

        // 4. Dispatch to Delivery Channels
        deliverEmail(email, event.getTitle(), event.getMessage());
        deliverSMS(phone, recipient.getUsername(), event.getTitle(), event.getMessage());
        deliverPushNotification(recipient.getUsername(), recipient.getRole(), event.getTitle(), event.getMessage(), event.getType(), event.getPriority());
    }

    @Async
    @EventListener
    public void handleAuthenticationSuccess(org.springframework.security.authentication.event.AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = "N/A";
        Object details = event.getAuthentication().getDetails();
        if (details instanceof org.springframework.security.web.authentication.WebAuthenticationDetails) {
            ipAddress = ((org.springframework.security.web.authentication.WebAuthenticationDetails) details).getRemoteAddress();
        }
        
        logger.info("Authentication success event for user: {} from IP: {}", username, ipAddress);

        // Generate dynamic notification event
        NotificationEvent notifEvent = new NotificationEvent(
            this,
            username,
            "Login Success",
            "A successful login was detected from IP: " + ipAddress + ".",
            "SYSTEM",
            "LOW"
        );
        handleNotificationEvent(notifEvent);
    }

    @Async
    @EventListener
    public void handleAuthenticationFailure(org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = "N/A";
        Object details = event.getAuthentication().getDetails();
        if (details instanceof org.springframework.security.web.authentication.WebAuthenticationDetails) {
            ipAddress = ((org.springframework.security.web.authentication.WebAuthenticationDetails) details).getRemoteAddress();
        }
        String reason = event.getException().getMessage();
        logger.info("Authentication failure event for user: {} from IP: {}, reason: {}", username, ipAddress, reason);

        Optional<User> recipientOpt = userRepository.findByUsername(username);
        if (recipientOpt.isPresent()) {
            NotificationEvent notifEvent = new NotificationEvent(
                this,
                username,
                "Failed Login Attempt",
                "A failed login attempt was detected from IP: " + ipAddress + ". Reason: " + reason,
                "SYSTEM",
                "HIGH"
            );
            handleNotificationEvent(notifEvent);
        } else {
            // Target admin directly for unrecognized username
            NotificationEvent notifEvent = new NotificationEvent(
                this,
                "admin",
                "System Alert: Unrecognized Failed Login",
                "A failed login attempt was made with unrecognized username '" + username + "' from IP: " + ipAddress + ". Reason: " + reason,
                "SYSTEM",
                "HIGH"
            );
            handleNotificationEvent(notifEvent);
        }
    }

    private void deliverEmail(String email, String title, String message) {
        if (mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(email);
                mailMessage.setSubject(title);
                mailMessage.setText(message);
                mailMessage.setFrom("notifications@logitrackpro.com");
                mailSender.send(mailMessage);
                logger.info("Email notification successfully sent to: {}", email);
            } catch (Exception e) {
                logger.error("Failed to send real email to {}: {}", email, e.getMessage());
                logFallbackEmail(email, title, message);
            }
        } else {
            logFallbackEmail(email, title, message);
        }
    }

    private void logFallbackEmail(String email, String title, String message) {
        String mockEmail = String.format(
            "\n================ SMTP EMAIL SERVER GATEWAY ================\n" +
            "TO: %s\n" +
            "SUBJECT: %s\n" +
            "BODY:\n%s\n" +
            "===========================================================",
            email, title, message
        );
        logger.info(mockEmail);
        System.out.println(mockEmail);
    }

    private void deliverSMS(String phone, String username, String title, String message) {
        String targetPhone = phone != null ? phone : "N/A (No registration)";
        String mockSMS = String.format(
            "\n================ SMS NOTIFICATION GATEWAY ================\n" +
            "TO PHONE: %s (Username: %s)\n" +
            "TEXT: [%s] - %s\n" +
            "===========================================================",
            targetPhone, username, title, message
        );
        logger.info(mockSMS);
        System.out.println(mockSMS);
    }

    private void deliverPushNotification(String username, String role, String title, String message, String type, String priority) {
        String mockPush = String.format(
            "\n================ ANDROID FCM PUSH GATEWAY ================\n" +
            "TO USER: %s (%s)\n" +
            "PAYLOAD: {\n" +
            "  \"title\": \"%s\",\n" +
            "  \"message\": \"%s\",\n" +
            "  \"type\": \"%s\",\n" +
            "  \"priority\": \"%s\"\n" +
            "}\n" +
            "===========================================================",
            username, role, title, message, type, priority
        );
        logger.info(mockPush);
        System.out.println(mockPush);
    }
}
