package com.formation.qualite.boutique.service.notification;

import com.formation.qualite.boutique.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation par email, utilisee pour les communications
 * transactionnelles (confirmation de paiement, suivi d'expedition).
 */
public class EmailOrderNotifier implements OrderNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOrderNotifier.class);

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
        // Pas d'email d'annulation pour le moment. Lever une exception ici
        // ferait echouer une annulation deja enregistree : on se contente
        // de tracer, pour respecter le contrat de OrderNotifier.
        LOGGER.info("Pas d'email d'annulation pour la commande {}", order.getId());
    }

    private static void send(String email, String subject) {
        LOGGER.info("Email a {} : {}", email, subject);
    }
}
