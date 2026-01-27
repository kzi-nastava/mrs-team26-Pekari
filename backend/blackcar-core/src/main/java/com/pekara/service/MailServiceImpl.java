package com.pekara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@blackcar.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Async
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

    @Async
    @Override
    public void sendDriverActivationEmail(String toEmail, String activationToken, String driverName) {
        try {
            String activationLink = frontendUrl + "/activate?token=" + activationToken + "&mode=driver";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to BlackCar - Activate Your Driver Account");
            message.setText(
                    "Hello " + driverName + ",\n\n" +
                    "You have been registered as a driver on BlackCar!\n\n" +
                    "Please click the link below to activate your account and set up your password:\n" +
                    activationLink + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
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

    @Async
    @Override
    public void sendRideAssignedToDriver(String driverEmail, Long rideId, java.time.LocalDateTime scheduledAt) {
        String when = scheduledAt == null ? "now" : "scheduled for " + scheduledAt;
        sendPlainText(
                driverEmail,
                "New Ride Assigned (Ride #" + rideId + ")",
                "You have been assigned a new ride (Ride #" + rideId + ") " + when + ".\n\n" +
                "Please check the application for details.\n\n" +
                "BlackCar Team"
        );
    }

    @Async
    @Override
    public void sendRideOrderAccepted(String toEmail, Long rideId, String status) {
        sendPlainText(
                toEmail,
                "Ride Order Accepted (Ride #" + rideId + ")",
                "Your ride order has been accepted.\n\n" +
                "Ride ID: " + rideId + "\n" +
                "Status: " + status + "\n\n" +
                "BlackCar Team"
        );
    }

    @Async
    @Override
    public void sendRideOrderRejected(String toEmail, String reason) {
        sendPlainText(
                toEmail,
                "Ride Order Rejected",
                "Your ride order could not be completed.\n\n" +
                "Reason: " + reason + "\n\n" +
                "BlackCar Team"
        );
    }

    @Async
    @Override
    public void sendRideDetailsShared(String toEmail, Long rideId, String creatorEmail) {
        sendPlainText(
                toEmail,
                "Ride Details Shared (Ride #" + rideId + ")",
                creatorEmail + " shared ride details with you.\n\n" +
                "Ride ID: " + rideId + "\n\n" +
                "BlackCar Team"
        );
    }

    @Async
    @Override
    public void sendRideReminder(String toEmail, Long rideId, java.time.LocalDateTime scheduledAt) {
        sendPlainText(
                toEmail,
                "Ride Reminder (Ride #" + rideId + ")",
                "Reminder: your scheduled ride (Ride #" + rideId + ") starts at " + scheduledAt + ".\n\n" +
                "BlackCar Team"
        );
    }

    private void sendPlainText(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
