package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator pricingCalculator = new PricingCalculator();

    @ParameterizedTest(name = "{0} brut={1} -> remise={2} livraison={3} final={4}")
    @CsvSource({
            "STANDARD, 999.99, 0, 0, 999.99",
            "STANDARD, 1000, 0, 0, 1000",
            "STANDARD, 1000.01, 50.0005, 0, 950.0095",
            "PREMIUM, 999.99, 99.99900000000001, 0, 899.991",
            "PREMIUM, 1000, 100, 0, 900",
            "PREMIUM, 1000.01, 150.0015, 0, 850.0085",
            "STANDARD, 500, 0, 20, 520",
            "STANDARD, 500.01, 0, 0, 500.01",
            "PREMIUM, 550, 55, 20, 515",
            "PREMIUM, 600, 60, 0, 540",
            ", 1000.01, 50.0005, 0, 950.0095"
    })
    void compute_shouldApplyDiscountThenShippingRules(CustomerType type, double grossAmount,
            double expectedDiscount, double expectedShipping, double expectedFinal) {
        Customer customer = new Customer("Client test", "client.test@example.com", type);

        PricingResult result = pricingCalculator.compute(customer, grossAmount);

        assertThat(result.grossAmount()).isEqualTo(grossAmount);
        assertThat(result.discountAmount()).isEqualTo(expectedDiscount);
        assertThat(result.shippingFee()).isEqualTo(expectedShipping);
        assertThat(result.finalAmount()).isEqualTo(expectedFinal);
    }
}
