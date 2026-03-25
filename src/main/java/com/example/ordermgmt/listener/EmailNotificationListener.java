package com.example.ordermgmt.listener;

import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Intercepts generic EmailDispatchEvents, renders the specified Thymeleaf
 * template
 * using the provided data Map, and dispatches the HTML to the pristine
 * EmailService.
 */
@Component
public class EmailNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final TemplateEngine templateEngine;
    private final EmailService emailService;

    public EmailNotificationListener(TemplateEngine templateEngine, EmailService emailService) {
        this.templateEngine = templateEngine;
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEmailDispatch(EmailDispatchEvent event) {
        logger.info("Intercepted EmailDispatchEvent handling template [{}] for recipient [{}]",
                event.templateName(), event.recipientEmail());

        // Restore Tenant Context for the async thread to ensure EmailLog correctly
        // attributes the org_id
        if (event.orgId() != null) {
            com.example.ordermgmt.security.TenantContextHolder.setTenantId(event.orgId());
        }

        try {
            // 1. Load the dynamic variables into a Thymeleaf context
            Context context = new Context();
            if (event.templateData() != null) {
                context.setVariables(event.templateData());
            }

            // 2. Render the final HTML string
            String htmlBody = templateEngine.process("emails/" + event.templateName(), context);

            // 3. Dispatch to the core mail service
            emailService.sendEmail(event.recipientEmail(), event.subject(), htmlBody);

        } catch (Exception e) {
            logger.error("Failed to generate and send email for template [{}] to recipient [{}]",
                    event.templateName(), event.recipientEmail(), e);
        } finally {
            com.example.ordermgmt.security.TenantContextHolder.clear();
        }
    }
}
