package com.ledgerly.notification.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends outbound emails. The default bean logs the email instead of contacting
 * an SMTP server — swap with a JavaMailSender-backed implementation in
 * production.
 */
public interface EmailSender {

    void send(String to, String subject, String body);

    @Component
    public class LoggingEmailSender implements EmailSender {

        private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

        @Override
        public void send(String to, String subject, String body) {
            log.info("EMAIL to='{}' subject='{}'", to, subject);
            log.debug("EMAIL body: {}", body);
        }
    }
}