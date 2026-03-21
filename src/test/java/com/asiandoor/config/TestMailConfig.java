package com.asiandoor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.internet.MimeMessage;

/**
 * Test-only mail sender to avoid external SMTP dependency in CI.
 */
@Configuration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                // no-op for tests
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
                // no-op for tests
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) {
                // no-op for tests
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) {
                // no-op for tests
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) {
                // no-op for tests
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
                // no-op for tests
            }
        };
    }
}
