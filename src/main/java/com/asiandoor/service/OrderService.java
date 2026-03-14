package com.asiandoor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asiandoor.entity.Order;
import com.asiandoor.entity.OrderItem;
import com.asiandoor.entity.OrderStatus;
import com.asiandoor.entity.Product;
import com.asiandoor.entity.User;
import com.asiandoor.repository.OrderRepository;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Transactional
    public Order placeOrder(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Map<Long, Integer> cart = cartService.getCartSnapshot(userId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock < quantity) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            double unitPrice = product.getPrice() == null ? 0.0 : product.getPrice();

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(unitPrice);
            items.add(item);

            product.setStock(stock - quantity);
            productRepository.save(product);

            total += unitPrice * quantity;
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart.");
        }

        order.setItems(items);
        order.setTotalPrice(total);

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return savedOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status is required.");
        }

        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
