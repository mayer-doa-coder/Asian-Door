package com.asiandoor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.asiandoor.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT LOWER(oi.product.category), SUM(oi.quantity)
            FROM OrderItem oi
            WHERE oi.order.user.id = :userId AND oi.product.category IS NOT NULL
            GROUP BY LOWER(oi.product.category)
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findPurchasedCategoryCountsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT oi.product.id FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<Long> findDistinctPurchasedProductIdsByUserId(@Param("userId") Long userId);
}
