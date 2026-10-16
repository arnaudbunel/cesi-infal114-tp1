package com.formation.qualite.boutique.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.formation.qualite.boutique.dto.CreateOrderRequest;
import com.formation.qualite.boutique.dto.OrderLineRequest;
import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;
import com.formation.qualite.boutique.model.OrderStatus;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.OrderRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.exception.InsufficientStockException;
import com.formation.qualite.boutique.service.exception.ResourceNotFoundException;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

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
        orderService = new OrderService(customerRepository, productRepository, orderRepository, notifier,
                new PricingCalculator());

        standardCustomer = new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD);
        standardCustomer.setId(1L);

        cheapProduct = new Product("Souris sans fil", 29.90, 75);
        cheapProduct.setId(10L);

        // Stub partage par les tests qui enregistrent une commande ; lenient
        // car les tests d'erreur s'arretent avant l'enregistrement.
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrder_standardCustomerSmallOrder_appliesShippingFeeAndNoDiscount() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 2)));

        Order order = orderService.createOrder(request);

        assertThat(order.getGrossAmount()).isEqualTo(59.80);
        assertThat(order.getDiscountAmount()).isZero();
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
        assertThat(order.getShippingFee()).isZero();
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
    void createOrder_withSeveralLines_shouldSumLinesAndDecrementEachStock() {
        Product keyboard = new Product("Clavier mecanique", 89.90, 40);
        keyboard.setId(11L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct, keyboard));
        CreateOrderRequest request = new CreateOrderRequest(1L,
                List.of(new OrderLineRequest(10L, 2), new OrderLineRequest(11L, 1)));

        Order order = orderService.createOrder(request);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getGrossAmount()).isEqualTo(149.7);
        assertThat(order.getCreatedDate()).isNotNull();
        assertThat(cheapProduct.getStock()).isEqualTo(73);
        assertThat(keyboard.getStock()).isEqualTo(39);
        verify(productRepository).save(cheapProduct);
        verify(productRepository).save(keyboard);
    }

    // Tests de caracterisation : figent les montants calcules par le code
    // actuel (seuils stricts, remise avant livraison, type null = STANDARD).
    @ParameterizedTest(name = "{0} brut={1} -> remise={2} livraison={3} final={4}")
    @CsvSource({
            "STANDARD, 999.99, 0, 0, 999.99",
            "STANDARD, 1000, 0, 0, 1000",
            "STANDARD, 1000.01, 50.0005, 0, 950.0095",
            "PREMIUM, 1000, 100, 0, 900",
            "PREMIUM, 1000.01, 150.0015, 0, 850.0085",
            "STANDARD, 500, 0, 20, 520",
            "STANDARD, 500.01, 0, 0, 500.01",
            "PREMIUM, 550, 55, 20, 515",
            ", 1000.01, 50.0005, 0, 950.0095"
    })
    void createOrder_pricingRules_shouldStayUnchanged(CustomerType type, double grossAmount,
            double expectedDiscount, double expectedShipping, double expectedFinal) {
        Customer customer = new Customer("Client test", "client.test@example.com", type);
        customer.setId(2L);
        Product product = new Product("Produit test", grossAmount, 10);
        product.setId(30L);

        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(productRepository.findAll()).thenReturn(List.of(product));

        CreateOrderRequest request = new CreateOrderRequest(2L, List.of(new OrderLineRequest(30L, 1)));

        Order order = orderService.createOrder(request);

        assertThat(order.getGrossAmount()).isEqualTo(grossAmount);
        assertThat(order.getDiscountAmount()).isEqualTo(expectedDiscount);
        assertThat(order.getShippingFee()).isEqualTo(expectedShipping);
        assertThat(order.getFinalAmount()).isEqualTo(expectedFinal);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, "La requete de commande est obligatoire"),
                Arguments.of(new CreateOrderRequest(null, List.of(new OrderLineRequest(10L, 1))),
                        "Le client est obligatoire"),
                Arguments.of(new CreateOrderRequest(1L, null), "Une commande doit contenir au moins une ligne"),
                Arguments.of(new CreateOrderRequest(1L, List.of()), "Une commande doit contenir au moins une ligne"),
                Arguments.of(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 0))),
                        "La quantite doit etre positive"),
                Arguments.of(new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, -1))),
                        "La quantite doit etre positive"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void createOrder_invalidRequest_shouldBeRejected(CreateOrderRequest request, String expectedMessage) {
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_unknownCustomer_shouldThrowNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        CreateOrderRequest request = new CreateOrderRequest(99L, List.of(new OrderLineRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Client introuvable : 99");
    }

    @Test
    void createOrder_unknownProduct_shouldThrowNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(999L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produit introuvable : 999");
    }

    @Test
    void createOrder_insufficientStock_shouldNotChangeStockNorSaveOrder() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 76)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Stock insuffisant pour le produit : Souris sans fil");
        assertThat(cheapProduct.getStock()).isEqualTo(75);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_stockUpdateFailure_shouldPropagateAndNotSaveOrder() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(standardCustomer));
        when(productRepository.findAll()).thenReturn(List.of(cheapProduct));
        when(productRepository.save(cheapProduct)).thenThrow(new DataAccessResourceFailureException("Base indisponible"));
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderLineRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("Base indisponible");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void payOrder_fromCreated_shouldChangeStatusToPaid() {
        Order order = new Order(standardCustomer);
        order.setId(100L);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order paid = orderService.payOrder(100L);

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(notifier).notifyPaid(paid);
    }

    @Test
    void payOrder_notCreated_shouldBeRejected() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PAID)));

        assertThatThrownBy(() -> orderService.payOrder(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Seule une commande CREATED peut etre payee");
        verify(notifier, never()).notifyPaid(any(Order.class));
    }

    @Test
    void shipOrder_fromPaid_shouldChangeStatusAndNotify() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PAID)));

        Order shipped = orderService.shipOrder(100L);

        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(notifier).notifyShipped(shipped);
    }

    @ParameterizedTest
    @CsvSource({
            "CANCELLED, Une commande annulee ne peut pas etre expediee",
            "CREATED, Seule une commande PAID peut etre expediee"
    })
    void shipOrder_notPaid_shouldBeRejected(OrderStatus status, String expectedMessage) {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(orderWithStatus(status)));

        assertThatThrownBy(() -> orderService.shipOrder(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);
        verify(notifier, never()).notifyShipped(any(Order.class));
    }

    @Test
    void cancelOrder_notShipped_shouldChangeStatusAndNotify() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PAID)));

        Order cancelled = orderService.cancelOrder(100L);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(notifier).notifyCancelled(cancelled);
    }

    @Test
    void cancelOrder_shipped_shouldBeRejected() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(orderWithStatus(OrderStatus.SHIPPED)));

        assertThatThrownBy(() -> orderService.cancelOrder(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Une commande expediee ne peut plus etre annulee");
        verify(notifier, never()).notifyCancelled(any(Order.class));
    }

    @Test
    void getAllOrders_shouldReturnRepositoryContent() {
        List<Order> orders = List.of(orderWithStatus(OrderStatus.CREATED));
        when(orderRepository.findAll()).thenReturn(orders);

        assertThat(orderService.getAllOrders()).isEqualTo(orders);
    }

    @Test
    void getOrderById_unknownId_shouldThrowNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Commande introuvable : 404");
    }

    @Test
    void generateOrderSummary_shouldListLinesAndFinalAmount() {
        Order order = orderWithStatus(OrderStatus.CREATED);
        order.addLine(new OrderLine(cheapProduct, 2, 29.90));
        order.setFinalAmount(79.8);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        String summary = orderService.generateOrderSummary(100L);

        assertThat(summary).isEqualTo("""
                Commande #100 - Client : Alice Martin
                Souris sans fil x2 = 59.8 EUR
                Total : 79.8 EUR""");
    }

    private Order orderWithStatus(OrderStatus status) {
        Order order = new Order(standardCustomer);
        order.setId(100L);
        order.setStatus(status);
        return order;
    }
}
