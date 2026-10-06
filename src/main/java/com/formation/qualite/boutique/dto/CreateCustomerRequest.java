package com.formation.qualite.boutique.dto;

import com.formation.qualite.boutique.model.CustomerType;

public record CreateCustomerRequest(String name, String email, CustomerType type) {
}
