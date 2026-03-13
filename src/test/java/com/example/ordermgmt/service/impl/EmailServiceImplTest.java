package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.entity.EmailLog;
import com.example.ordermgmt.exception.EmailSendingException;
import com.example.ordermgmt.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;
    private EmailLog emailLog;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        emailLog = new EmailLog();
        emailLog.setId(UUID.randomUUID());
        
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendEmail_WithValidData_SendsSuccessfully() {
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendEmail("test@example.com", "Test Subject", "Test Body");

        ArgumentCaptor<EmailLog> logCaptor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(logCaptor.capture());
        
        EmailLog savedLog = logCaptor.getAllValues().get(0);
        assertEquals("test@example.com", savedLog.getRecipient());
        assertEquals("Test Subject", savedLog.getSubject());
        assertEquals("PENDING", savedLog.getStatus());

        EmailLog updatedLog = logCaptor.getAllValues().get(1);
        assertEquals("SENT", updatedLog.getStatus());
    }

    @Test
    void sendEmail_WithEmptyRecipient_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.sendEmail("", "Subject", "Body"));
    }

    @Test
    void sendEmail_WithEmptySubject_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.sendEmail("test@example.com", "", "Body"));
    }

    @Test
    void sendEmail_WithEmptyBody_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.sendEmail("test@example.com", "Subject", ""));
    }

    @Test
    void sendEmail_WhenSendingFails_UpdatesLogWithError() {
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailSendingException.class, () ->
                emailService.sendEmail("test@example.com", "Subject", "Body"));

        ArgumentCaptor<EmailLog> logCaptor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(logCaptor.capture());
        
        EmailLog failedLog = logCaptor.getAllValues().get(1);
        assertEquals("FAILED", failedLog.getStatus());
        assertNotNull(failedLog.getErrorMessage());
    }

    @Test
    void sendEmail_WithHtmlBody_SendsCorrectly() {
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        String htmlBody = "<html><body><h1>Test</h1></body></html>";
        emailService.sendEmail("test@example.com", "HTML Subject", htmlBody);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
