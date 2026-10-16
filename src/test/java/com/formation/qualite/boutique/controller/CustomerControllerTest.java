package com.formation.qualite.boutique.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.service.CustomerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import(CustomerService.class)
class CustomerControllerTest {

    private static final String CUSTOMERS_URL = "/api/customers";
    private static final String CUSTOMER_URL = "/api/customers/{id}";

    private final MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;

    @Autowired
    CustomerControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getAll_shouldReturnCustomers() throws Exception {
        when(customerRepository.findAll())
                .thenReturn(List.of(new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD)));

        mockMvc.perform(get(CUSTOMERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice Martin"));
    }

    @Test
    void getById_shouldReturnCustomer() throws Exception {
        when(customerRepository.findById(4L))
                .thenReturn(Optional.of(new Customer("David Chen", "david.chen@example.com", CustomerType.PREMIUM)));

        mockMvc.perform(get(CUSTOMER_URL, 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PREMIUM"));
    }

    @Test
    void getById_unknownCustomer_shouldReturn404() throws Exception {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get(CUSTOMER_URL, 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Client introuvable : 404"));
    }

    @Test
    void create_shouldMapRequestToCustomer() throws Exception {
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Jean Dupont", "email": "jean.dupont@example.com", "type": "STANDARD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jean Dupont"))
                .andExpect(jsonPath("$.email").value("jean.dupont@example.com"))
                .andExpect(jsonPath("$.type").value("STANDARD"));
    }
}
