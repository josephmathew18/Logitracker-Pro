package com.delivery.event;

import org.springframework.context.ApplicationEvent;

public class NotificationEvent extends ApplicationEvent {
    private final String recipientUsername;
    private final String title;
    private final String message;
    private final String type;
    private final String priority;

    public NotificationEvent(Object source, String recipientUsername, String title, String message, String type, String priority) {
        super(source);
        this.recipientUsername = recipientUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getPriority() {
        return priority;
    }
}
