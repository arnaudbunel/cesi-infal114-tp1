package com.formation.qualite.boutique.config;

import com.formation.qualite.boutique.service.notification.EmailOrderNotifier;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class NotificationConfig {

    @Bean
    @Profile("email-notifications")
    public OrderNotifier emailOrderNotifier() {
        return new EmailOrderNotifier();
    }
}
