package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.entity.EmailLog;
import com.example.ordermgmt.exception.EmailSendingException;
import com.example.ordermgmt.repository.EmailLogRepository;
import com.example.ordermgmt.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import com.example.ordermgmt.enums.EmailStatus;
import java.util.UUID;
import com.example.ordermgmt.security.TenantContextHolder;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        logger.info("Processing sendEmail for User: {}", to);

        validateInput(to, subject, body);

        EmailLog emailLog = createLogEntry(to, subject);

        try {
            sendMimeMessage(to, subject, body);
            updateLogStatus(emailLog, EmailStatus.SENT, null);
            logger.info("sendEmail completed successfully for User: {}", to);
        } catch (Exception e) {
            logger.error("sendEmail failed for recipient: {}: {}", to, e.getMessage());
            updateLogStatus(emailLog, EmailStatus.FAILED, e.getMessage());
            throw new EmailSendingException("Failed to send email to " + to, e);
        }
    }

    private void validateInput(String to, String subject, String body) {
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be null or empty");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Email body cannot be null or empty");
        }
    }

    private EmailLog createLogEntry(String to, String subject) {
        EmailLog emailLog = new EmailLog();
        emailLog.setRecipient(to);
        emailLog.setSubject(subject);
        emailLog.setStatus(EmailStatus.PENDING.name());

        return emailLogRepository.save(emailLog);
    }

    private void sendMimeMessage(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to create mime message for " + to, e);
        }
    }

    private void updateLogStatus(EmailLog emailLog, EmailStatus status, String errorMessage) {
        emailLog.setStatus(status.name());
        if (errorMessage != null) {
            emailLog.setErrorMessage(errorMessage);
        }
        emailLogRepository.save(emailLog);
    }
}
