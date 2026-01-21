package com.pekara.service;

public interface MailService {

    /**
     * Send account activation email to user.
     *
     * @param toEmail recipient email address
     * @param activationToken activation token to include in email
     */
    void sendActivationEmail(String toEmail, String activationToken);

    /**
     * Send driver account activation email.
     * This is used when an admin creates a driver account.
     *
     * @param toEmail recipient email address
     * @param activationToken activation token to include in email
     * @param driverName driver's first name for personalization
     */
    void sendDriverActivationEmail(String toEmail, String activationToken, String driverName);
}
