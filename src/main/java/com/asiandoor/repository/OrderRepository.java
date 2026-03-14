package com.asiandoor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asiandoor.entity.Order;
import com.asiandoor.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    List<Order> findByUser(User user);

    List<Order> findByUserOrderByOrderDateDesc(User user);
}
