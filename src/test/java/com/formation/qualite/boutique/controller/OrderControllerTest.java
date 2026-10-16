package com.formation.qualite.boutique.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.formation.qualite.boutique.model.Customer;
import com.formation.qualite.boutique.model.CustomerType;
import com.formation.qualite.boutique.model.Order;
import com.formation.qualite.boutique.model.OrderLine;
import com.formation.qualite.boutique.model.OrderStatus;
import com.formation.qualite.boutique.model.Product;
import com.formation.qualite.boutique.repository.CustomerRepository;
import com.formation.qualite.boutique.repository.OrderRepository;
import com.formation.qualite.boutique.repository.ProductRepository;
import com.formation.qualite.boutique.service.InvoiceService;
import com.formation.qualite.boutique.service.OrderService;
import com.formation.qualite.boutique.service.PricingCalculator;
import com.formation.qualite.boutique.service.notification.OrderNotifier;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Controleur et vrais services ; seuls les repositories et le notifier
// (des interfaces) sont simules.
@WebMvcTest(OrderController.class)
@Import({OrderService.class, InvoiceService.class, PricingCalculator.class})
class OrderControllerTest {

    private static final String ORDERS_URL = "/api/orders";
    private static final String ORDER_URL = "/api/orders/{id}";
    private static final String STATUS_PATH = "$.status";
    private static final String ERROR_PATH = "$.error";

    private final MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderNotifier notifier;

    private Customer customer;

    @Autowired
    OrderControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        customer = new Customer("Alice Martin", "alice.martin@example.com", CustomerType.STANDARD);
        customer.setId(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    @Test
    void create_shouldReturnCreatedOrderWithAmounts() throws Exception {
        Product product = new Product("Souris sans fil", 29.90, 75);
        product.setId(10L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAll()).thenReturn(List.of(product));

        mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "lines": [{"productId": 10, "quantity": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath(STATUS_PATH).value("CREATED"))
                .andExpect(jsonPath("$.grossAmount").value(59.8))
                .andExpect(jsonPath("$.shippingFee").value(20.0));
    }

    @Test
    void create_invalidQuantity_shouldReturn400() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "lines": [{"productId": 10, "quantity": 0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("La quantite doit etre positive"));
    }

    @Test
    void getAll_shouldReturnOrders() throws Exception {
        when(orderRepository.findAll()).thenReturn(List.of(order(OrderStatus.CREATED), order(OrderStatus.PAID)));

        mockMvc.perform(get(ORDERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_shouldReturnOrder() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.PAID)));

        mockMvc.perform(get(ORDER_URL, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_PATH).value("PAID"));
    }

    @Test
    void getById_unknownOrder_shouldReturn404() throws Exception {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get(ORDER_URL, 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERROR_PATH).value("Commande introuvable : 404"));
    }

    @Test
    void pay_shouldReturnPaidOrderAndNotify() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.CREATED)));

        mockMvc.perform(post(ORDER_URL + "/pay", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_PATH).value("PAID"));
        verify(notifier).notifyPaid(any(Order.class));
    }

    @Test
    void ship_shouldReturnShippedOrder() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.PAID)));

        mockMvc.perform(post(ORDER_URL + "/ship", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_PATH).value("SHIPPED"));
    }

    @Test
    void ship_unpaidOrder_shouldReturn400() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.CREATED)));

        mockMvc.perform(post(ORDER_URL + "/ship", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERROR_PATH).value("Seule une commande PAID peut etre expediee"));
    }

    @Test
    void cancel_shouldReturnCancelledOrder() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.CREATED)));

        mockMvc.perform(post(ORDER_URL + "/cancel", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_PATH).value("CANCELLED"));
    }

    @Test
    void invoice_shouldReturnPlainTextInvoice() throws Exception {
        Order order = order(OrderStatus.PAID);
        order.addLine(new OrderLine(new Product("Webcam HD", 45.00, 25), 1, 45.00));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get(ORDER_URL + "/invoice", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(Matchers.startsWith("Facture - Commande #1")));
    }

    private Order order(OrderStatus status) {
        Order order = new Order(customer);
        order.setId(1L);
        order.setStatus(status);
        return order;
    }
}
