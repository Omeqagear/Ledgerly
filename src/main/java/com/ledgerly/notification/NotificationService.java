package com.ledgerly.notification;

import org.springframework.stereotype.Service;

/**
 * Public service for the notification module. Other modules never call this
 * directly — it exists so that future PCI/audit/console operations can send
 * ad-hoc notifications. The primary entry point for notifications is the
 * internal {@code NotificationEventListener} reacting to domain events.
 */
@Service
public class NotificationService {

    private final com.ledgerly.notification.internal.EmailSender emailSender;

    public NotificationService(com.ledgerly.notification.internal.EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        emailSender.send(to, subject, body);
    }
}