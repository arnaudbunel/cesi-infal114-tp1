package com.formation.qualite.boutique.dto;

import java.util.List;

public record CreateOrderRequest(Long customerId, List<OrderLineRequest> lines) {
}
