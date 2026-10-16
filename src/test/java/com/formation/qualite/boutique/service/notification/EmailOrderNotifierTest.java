package com.formation.qualite.boutique.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class EmailOrderNotifierTest {

    private final EmailOrderNotifier notifier = new EmailOrderNotifier();

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD));
        order.setId(7L);
    }

    @Test
    void notifyPaid_shouldSendPaymentConfirmation(CapturedOutput output) {
        notifier.notifyPaid(order);

        assertThat(output).contains("Email a alice.martin@example.com : Confirmation de paiement - commande 7");
    }

    @Test
    void notifyShipped_shouldSendShippingEmail(CapturedOutput output) {
        notifier.notifyShipped(order);

        assertThat(output).contains("Email a alice.martin@example.com : Votre commande 7 a ete expediee");
    }

    @Test
    void notifyCancelled_shouldNotMakeTheCancellationFail(CapturedOutput output) {
        assertThatNoException().isThrownBy(() -> notifier.notifyCancelled(order));

        assertThat(output).contains("Pas d'email d'annulation pour la commande 7");
    }
}
