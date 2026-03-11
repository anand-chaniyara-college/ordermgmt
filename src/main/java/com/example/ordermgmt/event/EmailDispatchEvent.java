package com.example.ordermgmt.event;

import java.util.Map;
import java.util.UUID;

/**
 * A generic event to trigger styled HTML emails dynamically.
 * Eliminates the need for multiple domain-specific event classes.
 *
 * @param recipientEmail Who receives the email.
 * @param subject        The subject of the email.
 * @param templateName   The name of the Thymeleaf HTML template (e.g.,
 *                       "order-receipt").
 * @param templateData   Dynamic variables to inject into the HTML template.
 */
public record EmailDispatchEvent(
        String recipientEmail,
        String subject,
        String templateName,
        UUID orgId,
        Map<String, Object> templateData) {
}
