package com.asiandoor.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.asiandoor.entity.Product;
import com.asiandoor.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
