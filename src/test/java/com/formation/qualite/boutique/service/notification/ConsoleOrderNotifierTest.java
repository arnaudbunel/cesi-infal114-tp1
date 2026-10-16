package com.formation.qualite.boutique.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.formation.qualite.boutique.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ConsoleOrderNotifierTest {

    private final ConsoleOrderNotifier notifier = new ConsoleOrderNotifier();

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(7L);
    }

    @Test
    void notifyPaid_shouldLogPaidNotification(CapturedOutput output) {
        notifier.notifyPaid(order);

        assertThat(output).contains("Notification : commande 7 payee");
    }

    @Test
    void notifyShipped_shouldLogShippedNotification(CapturedOutput output) {
        notifier.notifyShipped(order);

        assertThat(output).contains("Notification : commande 7 expediee");
    }

    @Test
    void notifyCancelled_shouldLogCancelledNotification(CapturedOutput output) {
        notifier.notifyCancelled(order);

        assertThat(output).contains("Notification : commande 7 annulee");
    }
}
