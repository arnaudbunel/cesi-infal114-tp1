package com.formation.qualite.boutique.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.ProductService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class ProductControllerTest {

    private static final String PRODUCTS_URL = "/api/products";
    private static final String PRODUCT_URL = "/api/products/{id}";

    private final MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    ProductControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getAll_shouldReturnProducts() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of(new Product("Clavier mecanique", 89.90, 40)));

        mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Clavier mecanique"));
    }

    @Test
    void getById_shouldReturnProduct() throws Exception {
        when(productRepository.findById(3L)).thenReturn(Optional.of(new Product("Ecran 27 pouces", 219.00, 15)));

        mockMvc.perform(get(PRODUCT_URL, 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));
    }

    @Test
    void getById_unknownProduct_shouldReturn404() throws Exception {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get(PRODUCT_URL, 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Produit introuvable : 404"));
    }

    @Test
    void create_shouldMapRequestToProduct() throws Exception {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Webcam HD", "unitPrice": 45.0, "stock": 25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Webcam HD"))
                .andExpect(jsonPath("$.unitPrice").value(45.0))
                .andExpect(jsonPath("$.stock").value(25));
    }
}
