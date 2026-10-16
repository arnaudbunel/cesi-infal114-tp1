package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;
import com.formation.qualite.boutique.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Tests de caracterisation : figent le texte exact de la facture
// (libelles, ordre des lignes, format des montants).
class InvoiceServiceTest {

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(new PricingCalculator());
    }

    @Test
    void generateInvoiceText_standardCustomer_shouldKeepExactFormat() {
        Customer customer = new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD);
        Order order = new Order(customer);
        order.setId(42L);
        order.addLine(new OrderLine(new Product("Souris sans fil", 29.90, 75), 2, 29.90));
        order.addLine(new OrderLine(new Product("Clavier mecanique", 89.90, 40), 1, 89.90));

        String invoice = invoiceService.generateInvoiceText(order);

        assertThat(invoice).isEqualTo("""
                Facture - Commande #42
                Client : Alice Martin (STANDARD)
                 - Souris sans fil x2 = 59.8 EUR
                 - Clavier mecanique x1 = 89.9 EUR
                Sous-total : 149.7 EUR
                Remise : 0.0 EUR
                Livraison : 20.0 EUR
                Total : 169.7 EUR""");
    }

    @Test
    void generateInvoiceText_premiumCustomerOver1000_shouldKeepExactFormat() {
        Customer customer = new Customer("David Chen", "david.chen@example.com", CustomerType.PREMIUM);
        Order order = new Order(customer);
        order.setId(43L);
        order.addLine(new OrderLine(new Product("Ecran 27 pouces", 219.00, 15), 4, 219.00));
        order.addLine(new OrderLine(new Product("Chaise de bureau", 349.00, 8), 1, 349.00));

        String invoice = invoiceService.generateInvoiceText(order);

        assertThat(invoice).isEqualTo("""
                Facture - Commande #43
                Client : David Chen (PREMIUM)
                 - Ecran 27 pouces x4 = 876.0 EUR
                 - Chaise de bureau x1 = 349.0 EUR
                Sous-total : 1225.0 EUR
                Remise : 183.75 EUR
                Livraison : 0.0 EUR
                Total : 1041.25 EUR""");
    }
}
