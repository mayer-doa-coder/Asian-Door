package com.asiandoor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.OrderCreateRequestDTO;
import com.asiandoor.dto.OrderItemSummaryDTO;
import com.asiandoor.dto.OrderDTO;
import com.asiandoor.entity.Order;
import com.asiandoor.entity.OrderItem;
import com.asiandoor.entity.OrderStatus;
import com.asiandoor.entity.Product;
import com.asiandoor.entity.User;
import com.asiandoor.exception.ResourceNotFoundException;
import com.asiandoor.repository.OrderRepository;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final ZoneId BANGLADESH_ZONE = ZoneId.of("Asia/Dhaka");
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String PAYMENT_BKASH = "BKASH";
    private static final String PAYMENT_CASH_ON_DELIVERY = "CASH_ON_DELIVERY";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Transactional
    public OrderDTO placeOrder(Long userId) {
        return placeOrder(userId, null);
    }

    @Transactional
    public OrderDTO placeOrder(Long userId, OrderCreateRequestDTO request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Map<Long, Integer> cart = cartService.getCartSnapshot(userId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerName(resolveCustomerName(user, request));
        order.setCustomerEmail(resolveCustomerEmail(user, request));
        order.setCustomerPhone(resolveCustomerPhone(request));
        order.setDeliveryAddress(resolveDeliveryAddress(request));
        order.setPaymentType(resolvePaymentType(request));

        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

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

        return toDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByUser(Long userId, int page, int size) {
        if (userId == null) {
            return Page.empty();
        }

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 4 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        return orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        return orderRepository.findAllByOrderByOrderDateDesc(pageable)
                .map(this::toDTO);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
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

    public OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        dto.setPaymentType(toPaymentLabel(order.getPaymentType()));
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        LocalDateTime bangladeshDateTime = toBangladeshDateTime(order.getOrderDate());
        dto.setOrderDateDisplay(ORDER_DATE_FORMAT.format(bangladeshDateTime));
        dto.setOrderTimeDisplay(ORDER_TIME_FORMAT.format(bangladeshDateTime));

        List<OrderItemSummaryDTO> itemSummaries = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(item -> {
                    OrderItemSummaryDTO itemDTO = new OrderItemSummaryDTO();
                    itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                    itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Product");
                    itemDTO.setProductCategory(item.getProduct() != null ? item.getProduct().getCategory() : null);
                    itemDTO.setImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null);
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setUnitPrice(item.getPrice());

                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    double unitPrice = item.getPrice() != null ? item.getPrice() : 0.0;
                    itemDTO.setLineTotal(quantity * unitPrice);
                    return itemDTO;
                }).toList();

        dto.setItems(itemSummaries);
        return dto;
    }

    private LocalDateTime toBangladeshDateTime(LocalDateTime orderDate) {
        LocalDateTime source = orderDate != null ? orderDate : LocalDateTime.now(ZoneOffset.UTC);
        return source.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BANGLADESH_ZONE)
                .toLocalDateTime();
    }

    private String resolveCustomerName(User user, OrderCreateRequestDTO request) {
        if (request != null && StringUtils.hasText(request.getCustomerName())) {
            return request.getCustomerName().trim();
        }
        return user.getName();
    }

    private String resolveCustomerEmail(User user, OrderCreateRequestDTO request) {
        if (request != null && StringUtils.hasText(request.getCustomerEmail())) {
            return request.getCustomerEmail().trim().toLowerCase();
        }
        return user.getEmail();
    }

    private String resolveCustomerPhone(OrderCreateRequestDTO request) {
        if (request != null && StringUtils.hasText(request.getCustomerPhone())) {
            return request.getCustomerPhone().trim();
        }
        return null;
    }

    private String resolveDeliveryAddress(OrderCreateRequestDTO request) {
        if (request != null && StringUtils.hasText(request.getDeliveryAddress())) {
            return request.getDeliveryAddress().trim();
        }
        return null;
    }

    private String resolvePaymentType(OrderCreateRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getPaymentType())) {
            return PAYMENT_CASH_ON_DELIVERY;
        }

        String paymentType = request.getPaymentType().trim().toUpperCase();
        if (PAYMENT_BKASH.equals(paymentType)) {
            return PAYMENT_BKASH;
        }

        return PAYMENT_CASH_ON_DELIVERY;
    }

    private String toPaymentLabel(String paymentType) {
        if (PAYMENT_BKASH.equalsIgnoreCase(paymentType)) {
            return "Bkash";
        }
        return "Cash on Delivery";
    }
}
