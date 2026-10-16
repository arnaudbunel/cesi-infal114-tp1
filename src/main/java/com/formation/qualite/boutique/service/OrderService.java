package com.formation.qualite.boutique.service;

import com.formation.qualite.boutique.dto.CreateOrderRequest;
import com.formation.qualite.boutique.dto.OrderLineRequest;
import com.formation.qualite.boutique.model.Customer;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderNotifier notifier;
    private final PricingCalculator pricingCalculator;

    public OrderService(CustomerRepository customerRepository, ProductRepository productRepository,
            OrderRepository orderRepository, OrderNotifier notifier, PricingCalculator pricingCalculator) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.notifier = notifier;
        this.pricingCalculator = pricingCalculator;
    }

    // Transactionnelle : la mise a jour des stocks et l'enregistrement de la
    // commande reussissent ou echouent ensemble.
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validateRequest(request);

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.customerId()));

        // Le catalogue est charge une fois puis parcouru pour chaque ligne,
        // ce qui evite un appel repository supplementaire par ligne.
        List<Product> catalog = productRepository.findAll();

        Order order = new Order(customer);
        order.setCreatedDate(LocalDateTime.now(Clock.systemUTC()));

        double grossAmount = 0;
        for (OrderLineRequest lineRequest : request.lines()) {
            Product product = findInCatalog(catalog, lineRequest.productId());
            checkStock(product, lineRequest.quantity());
            order.addLine(new OrderLine(product, lineRequest.quantity(), product.getUnitPrice()));
            grossAmount = grossAmount + (product.getUnitPrice() * lineRequest.quantity());
        }

        applyPricing(order, pricingCalculator.compute(customer, grossAmount));
        order.setStatus(OrderStatus.CREATED);

        int totalItems = countItems(order);
        decrementStock(order);

        Order saved = orderRepository.save(order);
        LOGGER.info("Commande creee : {} articles={} montant final={}",
                saved.getId(), totalItems, saved.getFinalAmount());

        return saved;
    }

    private static void validateRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requete de commande est obligatoire");
        }

        if (request.customerId() == null) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Une commande doit contenir au moins une ligne");
        }

        for (OrderLineRequest lineRequest : request.lines()) {
            if (lineRequest.quantity() <= 0) {
                throw new IllegalArgumentException("La quantite doit etre positive");
            }
        }
    }

    private static Product findInCatalog(List<Product> catalog, Long productId) {
        return catalog.stream()
                .filter(candidate -> candidate.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + productId));
    }

    private static void checkStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Stock insuffisant pour le produit : " + product.getName());
        }
    }

    private static void applyPricing(Order order, PricingResult pricing) {
        order.setGrossAmount(pricing.grossAmount());
        order.setDiscountAmount(pricing.discountAmount());
        order.setShippingFee(pricing.shippingFee());
        order.setFinalAmount(pricing.finalAmount());
    }

    private static int countItems(Order order) {
        return order.getLines().stream()
                .mapToInt(OrderLine::getQuantity)
                .sum();
    }

    // Une erreur d'enregistrement du stock n'est plus ignoree : elle remonte
    // et annule la transaction, au lieu de creer une commande incoherente.
    private void decrementStock(Order order) {
        for (OrderLine line : order.getLines()) {
            Product product = line.getProduct();
            product.setStock(product.getStock() - line.getQuantity());
            productRepository.save(product);
        }
    }

    public Order payOrder(Long id) {
        Order order = getOrderById(id);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Seule une commande CREATED peut etre payee");
        }
        order.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(order);
        notifier.notifyPaid(saved);
        return saved;
    }

    public Order shipOrder(Long id) {
        Order order = getOrderById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Une commande annulee ne peut pas etre expediee");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Seule une commande PAID peut etre expediee");
        }
        order.setStatus(OrderStatus.SHIPPED);
        Order saved = orderRepository.save(order);
        notifier.notifyShipped(saved);
        return saved;
    }

    public Order cancelOrder(Long id) {
        Order order = getOrderById(id);
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Une commande expediee ne peut plus etre annulee");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        notifier.notifyCancelled(saved);
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
    }

    public String generateOrderSummary(Long id) {
        Order order = getOrderById(id);
        InvoiceFormatter formatter = new InvoiceFormatter();

        StringBuilder sb = new StringBuilder();
        sb.append(formatter.formatHeader(order)).append("\n");
        for (OrderLine line : order.getLines()) {
            sb.append(formatter.formatLine(line)).append("\n");
        }
        sb.append("Total : ").append(order.getFinalAmount()).append(" EUR");

        return sb.toString();
    }
}
