package com.asiandoor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asiandoor.entity.Order;
import com.asiandoor.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrder(Order order);
}
