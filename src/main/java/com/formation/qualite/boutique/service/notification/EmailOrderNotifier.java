package com.formation.qualite.boutique.service.notification;

import com.formation.qualite.boutique.model.Order;

/**
 * Implementation par email, utilisee pour les communications
 * transactionnelles (confirmation de paiement, suivi d'expedition).
 */
public class EmailOrderNotifier implements OrderNotifier {

    @Override
    public void notifyPaid(Order order) {
        send(order.getCustomer().getEmail(), "Confirmation de paiement - commande " + order.getId());
    }

    @Override
    public void notifyShipped(Order order) {
        send(order.getCustomer().getEmail(), "Votre commande " + order.getId() + " a ete expediee");
    }

    @Override
    public void notifyCancelled(Order order) {
        throw new UnsupportedOperationException("Pas d'email d'annulation pour le moment");
    }

    private void send(String email, String subject) {
        System.out.println("Email a " + email + " : " + subject);
    }
}
