package com.asiandoor.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.ProductDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 9;

    private final ProductRepository productRepository;

    // ── Create ───────────────────────────────────────────────────────────────

    public ProductDTO createProduct(ProductDTO dto) {
        return toDTO(productRepository.save(toEntity(dto)));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<ProductDTO> getAllProductDTOs() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                                .stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
    }

    public Optional<ProductDTO> getProductById(Long id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    /**
     * Returns a page of DTOs filtered by optional category and/or keyword.
     * Results are sorted by id descending (newest first).
     */
    public Page<ProductDTO> getFilteredProducts(String category, String search, int page) {
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasSearch   = StringUtils.hasText(search);

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));

        Page<Product> result;
        if (hasCategory && hasSearch) {
            result = productRepository.findByCategoryAndKeyword(category.trim(), search.trim(), pageable);
        } else if (hasCategory) {
            result = productRepository.findByCategoryIgnoreCase(category.trim(), pageable);
        } else if (hasSearch) {
            result = productRepository.searchByKeyword(search.trim(), pageable);
        } else {
            result = productRepository.findAll(pageable);
        }

        return result.map(this::toDTO);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        existing.setName(dto.getName());
        existing.setCategory(dto.getCategory());
        existing.setMaterial(dto.getMaterial());
        existing.setPrice(dto.getPrice());
        existing.setDimensions(dto.getDimensions());
        existing.setStock(dto.getStock());
        existing.setImageUrl(dto.getImageUrl());
        existing.setDescription(dto.getDescription());
        return toDTO(productRepository.save(existing));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    public ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCategory(p.getCategory());
        dto.setMaterial(p.getMaterial());
        dto.setPrice(p.getPrice());
        dto.setDimensions(p.getDimensions());
        dto.setStock(p.getStock());
        dto.setImageUrl(p.getImageUrl());
        dto.setDescription(p.getDescription());
        return dto;
    }

    private Product toEntity(ProductDTO dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setCategory(dto.getCategory());
        p.setMaterial(dto.getMaterial());
        p.setPrice(dto.getPrice());
        p.setDimensions(dto.getDimensions());
        p.setStock(dto.getStock());
        p.setImageUrl(dto.getImageUrl());
        p.setDescription(dto.getDescription());
        return p;
    }
}
