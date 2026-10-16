package com.formation.qualite.boutique.service.notification;

import com.formation.qualite.boutique.model.Order;
import org.springframework.stereotype.Component;

@Component
public class ConsoleOrderNotifier implements OrderNotifier {

    private static final String NOTIFICATION_PREFIX = "Notification : commande ";
    @Override
    public void notifyPaid(Order order) {
        System.out.println(NOTIFICATION_PREFIX + order.getId() + " payee");
    }

    @Override
    public void notifyShipped(Order order) {
        System.out.println(NOTIFICATION_PREFIX + order.getId() + " expediee");
    }

    @Override
    public void notifyCancelled(Order order) {
        System.out.println(NOTIFICATION_PREFIX + order.getId() + " annulee");
    }
}
