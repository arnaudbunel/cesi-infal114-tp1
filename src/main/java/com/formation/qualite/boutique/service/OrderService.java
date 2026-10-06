package com.formation.qualite.boutique.service;

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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderNotifier notifier;

    public OrderService(CustomerRepository customerRepository, ProductRepository productRepository,
            OrderRepository orderRepository, OrderNotifier notifier) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.notifier = notifier;
    }

    public Order createOrder(CreateOrderRequest request) {
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

        Customer c = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.customerId()));

        // Le catalogue est charge une fois puis parcouru pour chaque ligne,
        // ce qui evite un appel repository supplementaire par ligne.
        List<Product> catalog = productRepository.findAll();

        Order order = new Order(c);
        order.setCreatedDate(LocalDateTime.now());

        double grossAmount = 0;

        for (OrderLineRequest lineRequest : request.lines()) {
            Product p = null;
            for (Product candidate : catalog) {
                if (candidate.getId().equals(lineRequest.productId())) {
                    p = candidate;
                    break;
                }
            }

            if (p == null) {
                throw new ResourceNotFoundException("Produit introuvable : " + lineRequest.productId());
            }

            if (p.getStock() < lineRequest.quantity()) {
                throw new InsufficientStockException("Stock insuffisant pour le produit : " + p.getName());
            }

            OrderLine line = new OrderLine(p, lineRequest.quantity(), p.getUnitPrice());
            order.addLine(line);

            grossAmount = grossAmount + (p.getUnitPrice() * lineRequest.quantity());
        }

        order.setGrossAmount(grossAmount);

        double[] amounts = calc(c, grossAmount);
        double discountAmount = amounts[0];
        double shippingFee = amounts[1];
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(grossAmount - discountAmount + shippingFee);
        order.setStatus(OrderStatus.CREATED);

        int totalItems = 0;
        for (OrderLine line : order.getLines()) {
            totalItems = totalItems + line.getQuantity();
        }

        for (OrderLine line : order.getLines()) {
            Product p = line.getProduct();
            try {
                p.setStock(p.getStock() - line.getQuantity());
                productRepository.save(p);
            } catch (Exception e) {
                System.out.println("Erreur lors de la mise a jour du stock pour " + p.getName());
            }
        }

        Order saved = orderRepository.save(order);
        System.out.println("Commande creee : " + saved.getId()
                + " articles=" + totalItems
                + " montant final=" + saved.getFinalAmount());

        return saved;
    }

    private double[] calc(Customer c, double total) {
        double rate;
        if (c.getType() == CustomerType.PREMIUM) {
            if (total > 1000) {
                rate = 0.15;
            } else {
                rate = 0.10;
            }
        } else {
            if (total > 1000) {
                rate = 0.05;
            } else {
                rate = 0;
            }
        }

        double discountAmount = total * rate;
        double tmp = total - discountAmount;

        double shippingFee;
        if (tmp > 500) {
            shippingFee = 0;
        } else {
            shippingFee = 20;
        }

        return new double[] {discountAmount, shippingFee};
    }

    /**
     * Ancien mode de calcul de remise, conserve lors de la migration
     * vers le bareme par palier de 2022.
     */
    private double calculateLegacyDiscount(double total) {
        if (total > 500) {
            return total * 0.07;
        }
        return 0;
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
        List<Order> result = orderRepository.findAll();
        return result;
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
