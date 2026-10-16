package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ProductServiceTest {

    private static final String FREE_PRODUCT_WARNING = "Produit cree a prix nul";
    private static final String KEYBOARD = "Clavier";

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createProduct_shouldPersistWithStockAndPrice() {
        Product product = new Product(KEYBOARD, 89.90, 40);
        when(productRepository.save(product)).thenReturn(product);

        Product saved = productService.createProduct(product);

        assertThat(saved.getStock()).isEqualTo(40);
        assertThat(saved.getUnitPrice()).isEqualTo(89.90);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0, 0.004})
    void createProduct_zeroPriceAtTheCent_shouldWarn(double unitPrice, CapturedOutput output) {
        Product product = new Product("Echantillon", unitPrice, 10);
        when(productRepository.save(product)).thenReturn(product);

        productService.createProduct(product);

        assertThat(output).contains(FREE_PRODUCT_WARNING + " : Echantillon");
    }

    @Test
    void createProduct_oneCentPrice_shouldNotWarn(CapturedOutput output) {
        Product product = new Product("Autocollant", 0.01, 10);
        when(productRepository.save(product)).thenReturn(product);

        productService.createProduct(product);

        assertThat(output).doesNotContain(FREE_PRODUCT_WARNING);
    }

    @Test
    void getAllProducts_shouldReturnRepositoryContent() {
        List<Product> products = List.of(new Product(KEYBOARD, 89.90, 40));
        when(productRepository.findAll()).thenReturn(products);

        assertThat(productService.getAllProducts()).isEqualTo(products);
    }

    @Test
    void getProductById_shouldReturnProduct() {
        Product product = new Product(KEYBOARD, 89.90, 40);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.getProductById(1L)).isSameAs(product);
    }

    @Test
    void getProductById_unknownId_shouldThrowNotFound() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produit introuvable : 404");
    }
}
