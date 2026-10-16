package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;
import org.springframework.stereotype.Service;

/**
 * Generation d'un apercu de facture texte, independant du flux
 * de creation de commande (recalcule les montants a partir des lignes).
 */
@Service
public class InvoiceService {

    private static final String AMOUNT_LINE_END = " EUR\n";

    private final PricingCalculator pricingCalculator;

    public InvoiceService(PricingCalculator pricingCalculator) {
        this.pricingCalculator = pricingCalculator;
    }

    public String generateInvoiceText(Order order) {
        Customer customer = order.getCustomer();

        double total = 0;
        for (OrderLine line : order.getLines()) {
            total = total + (line.getUnitPrice() * line.getQuantity());
        }

        PricingResult pricing = pricingCalculator.compute(customer, total);

        StringBuilder invoice = new StringBuilder();
        invoice.append("Facture - Commande #").append(order.getId()).append("\n");
        invoice.append("Client : ").append(customer.getName()).append(" (").append(customer.getType()).append(")\n");
        for (OrderLine line : order.getLines()) {
            invoice.append(" - ").append(line.getProduct().getName())
                    .append(" x").append(line.getQuantity())
                    .append(" = ").append(line.getUnitPrice() * line.getQuantity()).append(AMOUNT_LINE_END);
        }
        invoice.append("Sous-total : ").append(pricing.grossAmount()).append(AMOUNT_LINE_END);
        invoice.append("Remise : ").append(pricing.discountAmount()).append(AMOUNT_LINE_END);
        invoice.append("Livraison : ").append(pricing.shippingFee()).append(AMOUNT_LINE_END);
        invoice.append("Total : ").append(pricing.finalAmount()).append(" EUR");

        return invoice.toString();
    }
}
