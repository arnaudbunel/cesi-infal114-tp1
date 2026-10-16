package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    // Un prix est considere nul s'il vaut 0,00 EUR une fois arrondi au
    // centime : comparer un double avec == serait fragile.
    private static final double ZERO_PRICE_TOLERANCE = 0.005;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        if (isFreeProduct(product)) {
            LOGGER.warn("Produit cree a prix nul : {}", product.getName());
        }
        return productRepository.save(product);
    }

    private static boolean isFreeProduct(Product product) {
        return Math.abs(product.getUnitPrice()) < ZERO_PRICE_TOLERANCE;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }
}
