package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void createCustomer_shouldPersistWithCorrectType() {
        CustomerService customerService = new CustomerService(customerRepository);
        Customer customer = new Customer("Jean Dupont", "jean.dupont@example.com", CustomerType.PREMIUM);
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer saved = customerService.createCustomer(customer);

        assertThat(saved.getType()).isEqualTo(CustomerType.PREMIUM);
        assertThat(saved.getName()).isEqualTo("Jean Dupont");
    }
}
