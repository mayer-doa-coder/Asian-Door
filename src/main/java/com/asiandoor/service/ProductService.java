package com.asiandoor.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.ProductDTO;
import com.asiandoor.entity.Product;
import com.asiandoor.repository.OrderItemRepository;
import com.asiandoor.repository.ProductRepository;
import com.asiandoor.repository.ProductReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 9;

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;

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

    public List<ProductDTO> getRelatedProductDTOs(Long currentProductId, String category, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<Product> allProducts = productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        Stream<Product> sameCategory = allProducts.stream()
                .filter(product -> !product.getId().equals(currentProductId))
                .filter(product -> StringUtils.hasText(product.getImageUrl()))
                .filter(product -> StringUtils.hasText(category)
                        && StringUtils.hasText(product.getCategory())
                        && product.getCategory().equalsIgnoreCase(category));

        List<ProductDTO> related = sameCategory
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());

        if (related.size() < limit) {
            List<Long> selectedIds = related.stream().map(ProductDTO::getId).collect(Collectors.toList());
            allProducts.stream()
                    .filter(product -> !product.getId().equals(currentProductId))
                    .filter(product -> StringUtils.hasText(product.getImageUrl()))
                    .filter(product -> !selectedIds.contains(product.getId()))
                    .limit(limit - related.size())
                    .map(this::toDTO)
                    .forEach(related::add);
        }

        return related;
    }

    public List<ProductDTO> getPopularProducts(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<Object[]> topRatedStats = productReviewRepository.findTopRatedProductStats(PageRequest.of(0, limit));
        if (topRatedStats.isEmpty()) {
            return getFilteredProducts(null, null, 0, "newest", null, null)
                    .getContent()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        List<Long> orderedProductIds = topRatedStats.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());

        Map<Long, Product> productById = productRepository.findAllById(orderedProductIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        Map<Long, Object[]> statsByProductId = topRatedStats.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> row));

        return orderedProductIds.stream()
                .map(productById::get)
                .filter(product -> product != null)
                .map(this::toDTO)
                .peek(dto -> {
                    Object[] stats = statsByProductId.get(dto.getId());
                    double average = stats != null && stats[1] != null ? ((Number) stats[1]).doubleValue() : 0.0;
                    long total = stats != null && stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
                    dto.setAverageRating(average);
                    dto.setTotalReviews(total);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getRecommendedProductsForUser(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        List<Object[]> categoryPreferences = orderItemRepository.findPurchasedCategoryCountsByUserId(userId);
        if (categoryPreferences.isEmpty()) {
            return getFilteredProducts(null, null, 0, "newest", null, null)
                    .getContent()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        Set<Long> purchasedProductIds = new LinkedHashSet<>(orderItemRepository.findDistinctPurchasedProductIdsByUserId(userId));
        List<ProductDTO> recommended = new java.util.ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();

        for (Object[] preference : categoryPreferences) {
            if (recommended.size() >= limit) {
                break;
            }

            String category = preference[0] != null ? preference[0].toString() : null;
            if (!StringUtils.hasText(category)) {
                continue;
            }

            List<ProductDTO> categoryProducts = getFilteredProducts(category, null, 0, "newest", null, null).getContent();
            for (ProductDTO product : categoryProducts) {
                if (recommended.size() >= limit) {
                    break;
                }
                if (purchasedProductIds.contains(product.getId()) || selectedIds.contains(product.getId())) {
                    continue;
                }
                recommended.add(product);
                selectedIds.add(product.getId());
            }
        }

        if (recommended.size() < limit) {
            List<ProductDTO> fallback = getFilteredProducts(null, null, 0, "newest", null, null).getContent();
            for (ProductDTO product : fallback) {
                if (recommended.size() >= limit) {
                    break;
                }
                if (selectedIds.contains(product.getId()) || purchasedProductIds.contains(product.getId())) {
                    continue;
                }
                recommended.add(product);
                selectedIds.add(product.getId());
            }
        }

        return recommended;
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

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductDTO> productDTOs = productPage.getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        applyReviewStats(productDTOs);

        return new PageImpl<>(productDTOs, pageable, productPage.getTotalElements());
    }

    private void applyReviewStats(List<ProductDTO> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        List<Long> productIds = products.stream()
                .map(ProductDTO::getId)
                .collect(Collectors.toList());

        Map<Long, Object[]> statsByProductId = productReviewRepository.findReviewStatsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> row));

        for (ProductDTO product : products) {
            Object[] stats = statsByProductId.get(product.getId());
            if (stats == null) {
                product.setAverageRating(0.0);
                product.setTotalReviews(0L);
                continue;
            }

            double average = stats[1] != null ? ((Number) stats[1]).doubleValue() : 0.0;
            long total = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;

            product.setAverageRating(average);
            product.setTotalReviews(total);
        }
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
        existing.setLockSystem(dto.getLockSystem());
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
        dto.setLockSystem(p.getLockSystem());
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
        p.setLockSystem(dto.getLockSystem());
        p.setStock(dto.getStock());
        p.setImageUrl(dto.getImageUrl());
        p.setDescription(dto.getDescription());
        return p;
    }
}
