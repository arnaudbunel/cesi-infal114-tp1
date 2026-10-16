package com.formation.qualite.boutique.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.formation.qualite.boutique.service.notification.ConsoleOrderNotifier;
import com.formation.qualite.boutique.service.notification.EmailOrderNotifier;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// getBean(type) echoue si plusieurs beans du type existent : ces tests
// verifient qu'un seul OrderNotifier est actif pour chaque profil.
class NotificationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationConfig.class);

    @Test
    void defaultProfile_shouldUseConsoleNotifierOnly() {
        contextRunner.run(context -> assertThat(context)
                .getBean(OrderNotifier.class)
                .isInstanceOf(ConsoleOrderNotifier.class));
    }

    @Test
    void emailProfile_shouldUseEmailNotifierOnly() {
        contextRunner.withPropertyValues("spring.profiles.active=" + NotificationConfig.EMAIL_PROFILE)
                .run(context -> assertThat(context)
                        .getBean(OrderNotifier.class)
                        .isInstanceOf(EmailOrderNotifier.class));
    }
}
