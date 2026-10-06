package com.formation.qualite.boutique.repository;

import com.formation.qualite.boutique.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
