package com.pekara.service;

public interface MailService {

    /**
     * Send account activation email to user.
     *
     * @param toEmail recipient email address
     * @param activationToken activation token to include in email
     */
    void sendActivationEmail(String toEmail, String activationToken);
}
