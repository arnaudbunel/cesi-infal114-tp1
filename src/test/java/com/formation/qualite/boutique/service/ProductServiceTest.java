package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void createProduct_shouldPersistWithStockAndPrice() {
        ProductService productService = new ProductService(productRepository);
        Product product = new Product("Clavier", 89.90, 40);
        when(productRepository.save(product)).thenReturn(product);

        Product saved = productService.createProduct(product);

        assertThat(saved.getStock()).isEqualTo(40);
        assertThat(saved.getUnitPrice()).isEqualTo(89.90);
    }
}
