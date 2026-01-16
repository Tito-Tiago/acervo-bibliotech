package com.bibliotech.bibliotech.strategies.notifications;

import com.bibliotech.bibliotech.interfaces.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Estratégia de notificação via WhatsApp.
 * Implementa o padrão Strategy para envio de mensagens via WhatsApp.
 *
 * Esta implementação é um exemplo e pode ser substituída por
 * integrações reais com WhatsApp Business API, Twilio WhatsApp, etc.
 */
@Component
public class WhatsAppNotificationStrategy implements NotificationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationStrategy.class);

    private static final int MAX_MESSAGE_LENGTH = 4096;

    @Override
    public boolean send(String recipient, String subject, String message) {
        try {
            if (!isValidWhatsAppNumber(recipient)) {
                logger.error("Número de WhatsApp inválido: {}", recipient);
                return false;
            }

            String formattedMessage = formatWhatsAppMessage(subject, message);

            logger.info("Simulando envio de WhatsApp para: {}", recipient);
            logger.info("Mensagem formatada: {}", formattedMessage);

            return true;

        } catch (Exception e) {
            logger.error("Erro ao enviar WhatsApp para {}: {}", recipient, e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidWhatsAppNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        String cleaned = phone.replaceAll("[^0-9+]", "");

        return cleaned.startsWith("+") &&
                cleaned.length() >= 11 &&
                cleaned.length() <= 16;
    }

    private String formatWhatsAppMessage(String subject, String message) {
        StringBuilder formatted = new StringBuilder();

        if (subject != null && !subject.trim().isEmpty()) {
            formatted.append("*").append(subject).append("*\n\n");
        }

        if (message != null) {
            formatted.append(message);
        }

        String result = formatted.toString();

        if (result.length() > MAX_MESSAGE_LENGTH) {
            result = result.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }

        return result;
    }
}