package com.asiandoor.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.asiandoor.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query(value = "SELECT p FROM Product p WHERE " +
                   "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p FROM Product p WHERE " +
                   "LOWER(p.category) = LOWER(:category) AND (" +
                   "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "LOWER(p.category) = LOWER(:category) AND (" +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findByCategoryAndKeyword(@Param("category") String category,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);
}
