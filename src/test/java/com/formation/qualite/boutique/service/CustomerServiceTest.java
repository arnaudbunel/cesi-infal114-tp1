package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String NAME = "Jean Dupont";
    private static final String EMAIL = "jean.dupont@example.com";

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
    }

    @Test
    void createCustomer_shouldPersistWithCorrectType() {
        Customer customer = new Customer(NAME, EMAIL, CustomerType.PREMIUM);
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer saved = customerService.createCustomer(customer);

        assertThat(saved.getType()).isEqualTo(CustomerType.PREMIUM);
        assertThat(saved.getName()).isEqualTo(NAME);
    }

    @Test
    void getAllCustomers_shouldReturnRepositoryContent() {
        List<Customer> customers = List.of(new Customer(NAME, EMAIL, CustomerType.STANDARD));
        when(customerRepository.findAll()).thenReturn(customers);

        assertThat(customerService.getAllCustomers()).isEqualTo(customers);
    }

    @Test
    void getCustomerById_shouldReturnCustomer() {
        Customer customer = new Customer(NAME, EMAIL, CustomerType.STANDARD);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThat(customerService.getCustomerById(1L)).isSameAs(customer);
    }

    @Test
    void getCustomerById_unknownId_shouldThrowNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Client introuvable : 404");
    }
}
