package com.formation.qualite.boutique.repository;

import com.formation.qualite.boutique.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
