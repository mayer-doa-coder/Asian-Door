package com.asiandoor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asiandoor.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
