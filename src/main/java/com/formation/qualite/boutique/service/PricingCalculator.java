package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import org.springframework.stereotype.Component;

/**
 * Regle tarifaire unique, partagee par la creation de commande et la
 * facture : remise selon le type de client et le montant brut, puis
 * frais de livraison selon le montant apres remise.
 */
@Component
public class PricingCalculator {

    private static final double DISCOUNT_THRESHOLD = 1000;
    private static final double PREMIUM_HIGH_RATE = 0.15;
    private static final double PREMIUM_BASE_RATE = 0.10;
    private static final double STANDARD_HIGH_RATE = 0.05;
    private static final double STANDARD_BASE_RATE = 0;

    private static final double FREE_SHIPPING_THRESHOLD = 500;
    private static final double SHIPPING_FEE = 20;
    private static final double FREE_SHIPPING = 0;

    public PricingResult compute(Customer customer, double grossAmount) {
        double discountAmount = grossAmount * discountRate(customer, grossAmount);
        // Le montant apres remise est obtenu par soustraction, comme avant :
        // un autre ordre d'operations en double changerait les montants.
        double amountAfterDiscount = grossAmount - discountAmount;
        double shippingFee = amountAfterDiscount > FREE_SHIPPING_THRESHOLD ? FREE_SHIPPING : SHIPPING_FEE;
        return new PricingResult(grossAmount, discountAmount, shippingFee);
    }

    private static double discountRate(Customer customer, double grossAmount) {
        boolean aboveThreshold = grossAmount > DISCOUNT_THRESHOLD;
        // Comparaison par == : un client sans type est traite comme STANDARD.
        if (customer.getType() == CustomerType.PREMIUM) {
            return aboveThreshold ? PREMIUM_HIGH_RATE : PREMIUM_BASE_RATE;
        }
        return aboveThreshold ? STANDARD_HIGH_RATE : STANDARD_BASE_RATE;
    }
}
