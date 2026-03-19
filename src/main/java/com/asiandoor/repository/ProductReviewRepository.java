package com.asiandoor.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.asiandoor.entity.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);

    @Query("""
            SELECT pr.product.id, AVG(pr.rating), COUNT(pr.id)
            FROM ProductReview pr
            WHERE pr.product.id IN :productIds
            GROUP BY pr.product.id
            """)
    List<Object[]> findReviewStatsByProductIds(@Param("productIds") List<Long> productIds);

        @Query("""
            SELECT pr.product.id, AVG(pr.rating), COUNT(pr.id)
            FROM ProductReview pr
            GROUP BY pr.product.id
            ORDER BY AVG(pr.rating) DESC, COUNT(pr.id) DESC
            """)
        List<Object[]> findTopRatedProductStats(Pageable pageable);
}
