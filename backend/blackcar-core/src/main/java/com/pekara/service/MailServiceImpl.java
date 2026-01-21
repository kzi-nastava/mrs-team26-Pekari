package com.pekara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@blackcar.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendActivationEmail(String toEmail, String activationToken) {
        try {
            String activationLink = frontendUrl + "/activate?token=" + activationToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Activate Your BlackCar Account");
            message.setText(
                    "Welcome to BlackCar!\n\n" +
                    "Please click the link below to activate your account:\n" +
                    activationLink + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you didn't create an account, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "BlackCar Team"
            );

            mailSender.send(message);
            log.info("Activation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send activation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    @Override
    public void sendDriverActivationEmail(String toEmail, String activationToken, String driverName) {
        try {
            String activationLink = frontendUrl + "/activate?token=" + activationToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to BlackCar - Activate Your Driver Account");
            message.setText(
                    "Hello " + driverName + ",\n\n" +
                    "You have been registered as a driver on BlackCar!\n\n" +
                    "Please click the link below to activate your account and set up your password:\n" +
                    activationLink + "\n\n" +
                    "This link will expire in 48 hours.\n\n" +
                    "Once activated, you'll be able to start accepting rides and earning with BlackCar.\n\n" +
                    "If you didn't expect this email, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "BlackCar Team"
            );

            mailSender.send(message);
            log.info("Driver activation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send driver activation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send driver activation email", e);
        }
    }
}
