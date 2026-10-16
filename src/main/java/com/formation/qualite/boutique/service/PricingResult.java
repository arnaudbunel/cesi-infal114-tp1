package com.formation.qualite.boutique.service;

/**
 * Montants produits par la regle tarifaire a partir du montant brut :
 * remise, frais de livraison et montant final.
 */
public record PricingResult(double grossAmount, double discountAmount, double shippingFee) {

    public double finalAmount() {
        return grossAmount - discountAmount + shippingFee;
    }
}
