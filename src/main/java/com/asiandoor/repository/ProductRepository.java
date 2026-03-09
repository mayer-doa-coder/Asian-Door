package com.asiandoor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asiandoor.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
