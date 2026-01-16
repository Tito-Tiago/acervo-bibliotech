package com.bibliotech.bibliotech.strategies.notifications;

import com.bibliotech.bibliotech.interfaces.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SMSNotificationStrategy implements NotificationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SMSNotificationStrategy.class);

    @Override
    public boolean send(String recipient, String subject, String message) {
        try {
            if (!isValidPhoneNumber(recipient)) {
                logger.error("Número de telefone inválido: {}", recipient);
                return false;
            }

            logger.info("Simulando envio de SMS para: {}", recipient);
            logger.info("Assunto: {}", subject);
            logger.info("Mensagem: {}", truncateMessage(message, 160));

            return true;

        } catch (Exception e) {
            logger.error("Erro ao enviar SMS para {}: {}", recipient, e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        String cleaned = phone.replaceAll("[^0-9+]", "");

        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }

        if (message.length() <= maxLength) {
            return message;
        }

        return message.substring(0, maxLength - 3) + "...";
    }
}