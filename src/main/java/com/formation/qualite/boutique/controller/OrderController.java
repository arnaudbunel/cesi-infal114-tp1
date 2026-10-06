package com.formation.qualite.boutique.controller;

import com.formation.qualite.boutique.dto.CreateOrderRequest;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.service.InvoiceService;
import com.formation.qualite.boutique.service.OrderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public OrderController(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping("/{id}/pay")
    public Order pay(@PathVariable Long id) {
        return orderService.payOrder(id);
    }

    @PostMapping("/{id}/ship")
    public Order ship(@PathVariable Long id) {
        return orderService.shipOrder(id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping(value = "/{id}/invoice", produces = MediaType.TEXT_PLAIN_VALUE)
    public String invoice(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return invoiceService.generateInvoiceText(order);
    }
}
