package com.formation.qualite.boutique.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DataInitializerTest {

    private static final String TEST_VALUE = "valeur-de-test";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    void run_shouldSeedDemoCustomersAndProducts() {
        new DataInitializer(customerRepository, productRepository, TEST_VALUE).run();

        verify(customerRepository, times(5)).save(any(Customer.class));
        verify(productRepository, times(10)).save(any(Product.class));
    }

    @Test
    void run_withConfiguredKey_shouldSynchronizeWithoutLoggingTheKey(CapturedOutput output) {
        new DataInitializer(customerRepository, productRepository, TEST_VALUE).run();

        assertThat(output)
                .contains("Synchronisation programme fidelite")
                .doesNotContain(TEST_VALUE);
    }

    @Test
    void run_withoutKey_shouldSkipSynchronization(CapturedOutput output) {
        new DataInitializer(customerRepository, productRepository, "").run();

        assertThat(output).contains("Cle du programme fidelite absente : synchronisation ignoree");
    }
}
