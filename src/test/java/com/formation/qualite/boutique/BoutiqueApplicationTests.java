package com.formation.qualite.boutique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BoutiqueApplicationTests {

    private static final String ORDER_URL = "/api/orders/{id}";
    private static final String STATUS_PATH = "$.status";

    private final MockMvc mockMvc;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Autowired
    BoutiqueApplicationTests(MockMvc mockMvc, CustomerRepository customerRepository,
            ProductRepository productRepository) {
        this.mockMvc = mockMvc;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Test
    void contextLoads_shouldSeedDemoData() {
        assertThat(customerRepository.count()).isEqualTo(5);
        assertThat(productRepository.count()).isEqualTo(10);
    }

    // Parcours complet sur la vraie base H2 : verifie notamment que la
    // creation de commande enregistre bien la baisse de stock.
    @Test
    void orderLifecycle_shouldPersistOrderAndFollowStatusRules() throws Exception {
        Customer premiumCustomer = customerRepository.findAll().stream()
                .filter(customer -> customer.getType() == CustomerType.PREMIUM)
                .findFirst()
                .orElseThrow();
        Product product = productRepository.findAll().get(0);
        int initialStock = product.getStock();

        MvcResult creation = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "lines": [{"productId": %d, "quantity": 2}]}
                                """.formatted(premiumCustomer.getId(), product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(STATUS_PATH).value("CREATED"))
                .andReturn();
        Integer orderId = JsonPath.read(creation.getResponse().getContentAsString(), "$.id");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(initialStock - 2);

        mockMvc.perform(post(ORDER_URL + "/pay", orderId))
                .andExpect(jsonPath(STATUS_PATH).value("PAID"));
        mockMvc.perform(post(ORDER_URL + "/ship", orderId))
                .andExpect(jsonPath(STATUS_PATH).value("SHIPPED"));
        mockMvc.perform(post(ORDER_URL + "/cancel", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Une commande expediee ne peut plus etre annulee"));
        mockMvc.perform(get(ORDER_URL + "/invoice", orderId))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "Client : " + premiumCustomer.getName() + " (PREMIUM)")));
    }
}
