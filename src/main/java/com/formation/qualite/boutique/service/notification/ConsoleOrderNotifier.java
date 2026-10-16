package com.formation.qualite.boutique.service.notification;

import com.formation.qualite.boutique.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation par defaut : trace les notifications dans les logs.
 * Declaree comme bean par NotificationConfig.
 */
public class ConsoleOrderNotifier implements OrderNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleOrderNotifier.class);
    private static final String NOTIFICATION_MESSAGE = "Notification : commande {} {}";

    @Override
    public void notifyPaid(Order order) {
        LOGGER.info(NOTIFICATION_MESSAGE, order.getId(), "payee");
    }

    @Override
    public void notifyShipped(Order order) {
        LOGGER.info(NOTIFICATION_MESSAGE, order.getId(), "expediee");
    }

    @Override
    public void notifyCancelled(Order order) {
        LOGGER.info(NOTIFICATION_MESSAGE, order.getId(), "annulee");
    }
}
