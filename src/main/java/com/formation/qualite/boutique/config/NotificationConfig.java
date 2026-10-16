package com.formation.qualite.boutique.config;

import com.formation.qualite.boutique.service.notification.ConsoleOrderNotifier;
import com.formation.qualite.boutique.service.notification.EmailOrderNotifier;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Choix de l'unique implementation de OrderNotifier : console par defaut,
 * email lorsque le profil "email-notifications" est actif.
 */
@Configuration
public class NotificationConfig {

    static final String EMAIL_PROFILE = "email-notifications";

    @Bean
    @Profile("!" + EMAIL_PROFILE)
    public OrderNotifier consoleOrderNotifier() {
        return new ConsoleOrderNotifier();
    }

    @Bean
    @Profile(EMAIL_PROFILE)
    public OrderNotifier emailOrderNotifier() {
        return new EmailOrderNotifier();
    }
}
