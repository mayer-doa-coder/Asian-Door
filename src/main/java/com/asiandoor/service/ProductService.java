package com.asiandoor.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        return getFilteredProducts(category, search, page, "newest", null, null);
    }

    public Page<ProductDTO> getFilteredProducts(String category,
                                                String search,
                                                int page,
                                                String sort,
                                                Double minPrice,
                                                Double maxPrice) {
        Sort appliedSort = resolveSort(sort);
        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, PAGE_SIZE, appliedSort);

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            return Page.empty(pageable);
        }

        final Double normalizedMinPrice = minPrice;
        final Double normalizedMaxPrice = maxPrice;

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(category)) {
            String categoryNormalized = category.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category")), categoryNormalized));
        }

        if (StringUtils.hasText(search)) {
            String keyword = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword),
                    cb.like(cb.lower(root.get("material")), keyword)
            ));
        }

        if (normalizedMinPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), normalizedMinPrice));
        }

        if (normalizedMaxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), normalizedMaxPrice));
        }

        return productRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        productRepository.countProductsByCategory().forEach(row -> {
            String key = row[0] == null ? "" : row[0].toString();
            long value = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (!key.isBlank()) {
                counts.put(key, value);
            }
        });
        return counts;
    }

    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "id");
        }

        return switch (sort.trim().toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "id");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
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
