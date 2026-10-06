package com.formation.qualite.boutique.config;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DataInitializer(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        customerRepository.save(new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD));
        customerRepository.save(new Customer("Bruno Lefevre", "bruno.lefevre@example.com", CustomerType.STANDARD));
        customerRepository.save(new Customer("Chloe Dubois", "chloe.dubois@example.com", CustomerType.STANDARD));
        customerRepository.save(new Customer("David Chen", "david.chen@example.com", CustomerType.PREMIUM));
        customerRepository.save(new Customer("Emma Rousseau", "emma.rousseau@example.com", CustomerType.PREMIUM));

        productRepository.save(new Product("Clavier mecanique", 89.90, 40));
        productRepository.save(new Product("Souris sans fil", 29.90, 75));
        productRepository.save(new Product("Ecran 27 pouces", 219.00, 15));
        productRepository.save(new Product("Casque audio", 59.90, 30));
        productRepository.save(new Product("Webcam HD", 45.00, 25));
        productRepository.save(new Product("Station d'accueil USB-C", 129.90, 12));
        productRepository.save(new Product("Chaise de bureau", 349.00, 8));
        productRepository.save(new Product("Lampe de bureau LED", 24.90, 50));
        productRepository.save(new Product("Tapis de souris XL", 14.90, 60));
        productRepository.save(new Product("Disque SSD externe 1To", 99.00, 20));
    }
}
