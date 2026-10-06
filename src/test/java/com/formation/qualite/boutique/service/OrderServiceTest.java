package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.dto.CreateOrderRequest;
import com.formation.qualite.boutique.dto.OrderLineRequest;
import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderStatus;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.OrderRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderNotifier notifier;

    private OrderService orderService;

    private Customer standardCustomer;
    private Product cheapProduct;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(customerRepository, productRepository, orderRepository, notifier);

        standardCustomer = new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD);
        standardCustomer.setId(1L);

        cheapProduct = new Product("Souris sans fil", 29.90, 75);
        cheapProduct.setId(10L);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrder_standardCustomerSmallOrder_appliesShippingFeeAndNoDiscount() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 2)));

        Order order = orderService.createOrder(request);

        assertThat(order.getGrossAmount()).isEqualTo(59.80);
        assertThat(order.getDiscountAmount()).isEqualTo(0);
        assertThat(order.getShippingFee()).isEqualTo(20);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void createOrder_standardCustomerOver500_shouldHaveFreeShipping() {
        Product expensiveProduct = new Product("Ecran 27 pouces", 219.00, 15);
        expensiveProduct.setId(20L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(expensiveProduct));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(20L, 3)));

        Order order = orderService.createOrder(request);

        assertThat(order.getGrossAmount()).isEqualTo(657.00);
        assertThat(order.getShippingFee()).isEqualTo(0);
    }

    @Test
    void createOrder_shouldDecrementProductStock() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 5)));

        orderService.createOrder(request);

        assertThat(cheapProduct.getStock()).isEqualTo(70);
    }

    @Test
    void payOrder_fromCreated_shouldChangeStatusToPaid() {
        Order order = new Order(standardCustomer);
        order.setId(100L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order paid = orderService.payOrder(100L);

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
