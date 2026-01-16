package com.bibliotech.bibliotech.configuration;

import com.bibliotech.bibliotech.interfaces.NotificationStrategy;
import com.bibliotech.bibliotech.strategies.notifications.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class NotificationConfig {

    @Value("${notification.strategy:email}")
    private String notificationStrategy;

    @Value("${notification.multi.require-all-success:false}")
    private boolean requireAllSuccess;

    private final EmailNotificationStrategy emailStrategy;
    private final SMSNotificationStrategy smsStrategy;
    private final WhatsAppNotificationStrategy whatsAppStrategy;

    public NotificationConfig(
            EmailNotificationStrategy emailStrategy,
            SMSNotificationStrategy smsStrategy,
            WhatsAppNotificationStrategy whatsAppStrategy) {
        this.emailStrategy = emailStrategy;
        this.smsStrategy = smsStrategy;
        this.whatsAppStrategy = whatsAppStrategy;
    }

    @Bean
    @Primary
    public NotificationStrategy notificationStrategy() {
        return switch (notificationStrategy.toLowerCase()) {
            case "sms" -> {
                System.out.println("✓ Estratégia de notificação: SMS");
                yield smsStrategy;
            }
            case "whatsapp" -> {
                System.out.println("✓ Estratégia de notificação: WhatsApp");
                yield whatsAppStrategy;
            }
            case "multi" -> {
                System.out.println("✓ Estratégia de notificação: Multi-canal");
                yield createMultiChannelStrategy();
            }
            default -> {
                System.out.println("✓ Estratégia de notificação: Email (padrão)");
                yield emailStrategy;
            }
        };
    }

    private NotificationStrategy createMultiChannelStrategy() {
        MultiChannelNotificationStrategy multiChannel = new MultiChannelNotificationStrategy();

        multiChannel.addStrategy(emailStrategy);
        multiChannel.addStrategy(smsStrategy);
        multiChannel.addStrategy(whatsAppStrategy);

        multiChannel.setRequireAllSuccess(requireAllSuccess);

        System.out.println("  - Email: ativado");
        System.out.println("  - SMS: ativado");
        System.out.println("  - WhatsApp: ativado");
        System.out.println("  - Requer sucesso em todos: " + requireAllSuccess);

        return multiChannel;
    }

    @Bean(name = "emailNotificationStrategy")
    public NotificationStrategy emailNotificationStrategy() {
        return emailStrategy;
    }

    @Bean(name = "smsNotificationStrategy")
    public NotificationStrategy smsNotificationStrategy() {
        return smsStrategy;
    }

    @Bean(name = "whatsappNotificationStrategy")
    public NotificationStrategy whatsappNotificationStrategy() {
        return whatsAppStrategy;
    }
}