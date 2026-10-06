package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;
import org.springframework.stereotype.Service;

/**
 * Generation d'un apercu de facture texte, independant du flux
 * de creation de commande (recalcule les montants a partir des lignes).
 */
@Service
public class InvoiceService {

    public String generateInvoiceText(Order order) {
        Customer customer = order.getCustomer();

        double total = 0;
        for (OrderLine line : order.getLines()) {
            total = total + (line.getUnitPrice() * line.getQuantity());
        }

        double discountRate;
        if (customer.getType() == CustomerType.PREMIUM) {
            if (total > 1000) {
                discountRate = 0.15;
            } else {
                discountRate = 0.10;
            }
        } else {
            if (total > 1000) {
                discountRate = 0.05;
            } else {
                discountRate = 0;
            }
        }

        double discount = total * discountRate;
        double netAmount = total - discount;

        double shipping;
        if (netAmount > 500) {
            shipping = 0;
        } else {
            shipping = 20;
        }

        double invoiceTotal = netAmount + shipping;

        StringBuilder invoice = new StringBuilder();
        invoice.append("Facture - Commande #").append(order.getId()).append("\n");
        invoice.append("Client : ").append(customer.getName()).append(" (").append(customer.getType()).append(")\n");
        for (OrderLine line : order.getLines()) {
            invoice.append(" - ").append(line.getProduct().getName())
                    .append(" x").append(line.getQuantity())
                    .append(" = ").append(line.getUnitPrice() * line.getQuantity()).append(" EUR\n");
        }
        invoice.append("Sous-total : ").append(total).append(" EUR\n");
        invoice.append("Remise : ").append(discount).append(" EUR\n");
        invoice.append("Livraison : ").append(shipping).append(" EUR\n");
        invoice.append("Total : ").append(invoiceTotal).append(" EUR");

        return invoice.toString();
    }
}
