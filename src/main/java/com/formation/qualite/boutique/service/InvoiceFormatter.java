package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;

/**
 * Petit utilitaire de mise en forme texte, historiquement sorti
 * du module de facturation d'origine.
 */
public class InvoiceFormatter {

    public String formatHeader(Order order) {
        return "Commande #" + order.getId() + " - Client : " + order.getCustomer().getName();
    }

    public String formatLine(OrderLine line) {
        return line.getProduct().getName() + " x" + line.getQuantity()
                + " = " + (line.getQuantity() * line.getUnitPrice()) + " EUR";
    }
}
