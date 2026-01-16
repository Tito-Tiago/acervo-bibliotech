package com.bibliotech.bibliotech.strategies.notifications;

import com.bibliotech.bibliotech.interfaces.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MultiChannelNotificationStrategy implements NotificationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MultiChannelNotificationStrategy.class);

    private final List<NotificationStrategy> strategies;
    private boolean requireAllSuccess;

    public MultiChannelNotificationStrategy() {
        this.strategies = new ArrayList<>();
        this.requireAllSuccess = false;
    }

    public void addStrategy(NotificationStrategy strategy) {
        if (strategy != null && strategy != this) { // Evita recursão infinita
            strategies.add(strategy);
        }
    }

    public void removeStrategy(NotificationStrategy strategy) {
        strategies.remove(strategy);
    }

    public void setRequireAllSuccess(boolean requireAllSuccess) {
        this.requireAllSuccess = requireAllSuccess;
    }

    @Override
    public boolean send(String recipient, String subject, String message) {
        if (strategies.isEmpty()) {
            logger.warn("Nenhuma estratégia de notificação configurada");
            return false;
        }

        logger.info("Enviando notificação multicanal para: {}", recipient);

        int successCount = 0;
        int totalStrategies = strategies.size();

        for (NotificationStrategy strategy : strategies) {
            try {
                boolean success = strategy.send(recipient, subject, message);

                if (success) {
                    successCount++;
                    logger.info("Notificação enviada com sucesso via {}",
                            strategy.getClass().getSimpleName());
                } else {
                    logger.warn("Falha ao enviar notificação via {}",
                            strategy.getClass().getSimpleName());
                }

            } catch (Exception e) {
                logger.error("Erro ao enviar notificação via {}: {}",
                        strategy.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        logger.info("Resultado multicanal: {}/{} canais bem-sucedidos",
                successCount, totalStrategies);

        return requireAllSuccess
                ? successCount == totalStrategies
                : successCount > 0;
    }

    public int getStrategyCount() {
        return strategies.size();
    }
}