package com.formation.qualite.boutique.service.notification;

import com.formation.qualite.boutique.model.Order;

public interface OrderNotifier {

    void notifyPaid(Order order);

    void notifyShipped(Order order);

    void notifyCancelled(Order order);
}
