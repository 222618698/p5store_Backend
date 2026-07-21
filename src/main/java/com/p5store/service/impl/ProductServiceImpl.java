package com.p5store.service.impl;

import com.p5store.domain.Category;
import com.p5store.enums.ProductStatus;
import com.p5store.domain.Product;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.CategoryCountResponse;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.ReviewRepository;
import com.p5store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySku(req.sku()))
            throw new BusinessException("SKU already exists: " + req.sku());
        Category category = findCategory(req.categoryId());
        Product product = toEntity(new Product(), req, category);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product product = findProduct(id);
        if (!product.getSku().equals(req.sku()) && productRepository.existsBySku(req.sku()))
            throw new BusinessException("SKU already exists: " + req.sku());
        Category category = findCategory(req.categoryId());
        return toResponse(productRepository.save(toEntity(product, req, category)));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    @Override
    public ProductResponse getById(Long id) { return toResponse(findProduct(id)); }

    @Override
    public ProductResponse getBySku(String sku) {
        return toResponse(productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + sku)));
    }

    @Override
    public Page<ProductResponse> getAll(Pageable pageable) {
        Page<Product> page = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        Map<Long, ReviewRepository.ProductRatingAggregate> ratings = ratingsFor(page.getContent());
        return page.map(p -> toResponse(p, ratings));
    }

    @Override
    public List<ProductResponse> getFeatured() {
        return toResponseList(productRepository.findByFeaturedTrueAndStatus(ProductStatus.ACTIVE));
    }

    @Override
    public List<ProductResponse> getNewArrivals() {
        return toResponseList(productRepository.findNewArrivals(PageRequest.of(0, 8)));
    }

    @Override
    public List<ProductResponse> getByCategory(Long categoryId) {
        return toResponseList(productRepository.findByCategoryIdOrParentIdAndStatus(categoryId, ProductStatus.ACTIVE));
    }

    @Override
    public List<ProductResponse> search(String q) {
        return toResponseList(productRepository.search(q));
    }

    @Override
    public List<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max) {
        return toResponseList(productRepository.findByPriceRange(min, max));
    }

    @Override
    public List<CategoryCountResponse> getCategoryCounts() {
        return productRepository.countByTopLevelCategory().stream()
                .map(row -> new CategoryCountResponse(row.getCategoryId(), row.getCount()))
                .toList();
    }

    private Product toEntity(Product p, ProductRequest req, Category category) {
        p.setName(req.name());
        p.setDescription(req.description());
        p.setSku(req.sku());
        p.setPrice(req.price());
        p.setCompareAtPrice(req.compareAtPrice());
        p.setStockQuantity(req.stockQuantity());
        p.setImageUrl(req.imageUrl());
        p.setGalleryImages(req.galleryImages() != null ? new ArrayList<>(req.galleryImages()) : new ArrayList<>());
        p.setBrand(req.brand());
        p.setUnit(req.unit() == null || req.unit().isBlank() ? "Each" : req.unit());
        p.setBadge(req.badge());
        p.setFeatured(req.featured());
        p.setCategory(category);
        boolean wantsActive = req.active() == null || req.active();
        p.setStatus(!wantsActive
                ? ProductStatus.INACTIVE
                : (req.stockQuantity() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE));
        return p;
    }

    ProductResponse toResponse(Product p) {
        Double averageRating = reviewRepository.averageRatingByProductId(p.getId());
        long reviewCount = reviewRepository.countByProductIdAndApprovedTrue(p.getId());
        return toResponse(p, averageRating, reviewCount);
    }

    // List-returning endpoints must batch the rating lookup (1 query for the
    // whole list) rather than calling toResponse(Product) per item (2 queries
    // each) — with thousands of rows that N+1 pattern is severe.
    private List<ProductResponse> toResponseList(List<Product> products) {
        Map<Long, ReviewRepository.ProductRatingAggregate> ratings = ratingsFor(products);
        return products.stream().map(p -> toResponse(p, ratings)).toList();
    }

    private Map<Long, ReviewRepository.ProductRatingAggregate> ratingsFor(List<Product> products) {
        if (products.isEmpty()) return Map.of();
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, ReviewRepository.ProductRatingAggregate> map = new HashMap<>();
        for (ReviewRepository.ProductRatingAggregate agg : reviewRepository.findRatingAggregatesByProductIds(ids)) {
            map.put(agg.getProductId(), agg);
        }
        return map;
    }

    private ProductResponse toResponse(Product p, Map<Long, ReviewRepository.ProductRatingAggregate> ratings) {
        ReviewRepository.ProductRatingAggregate agg = ratings.get(p.getId());
        Double averageRating = agg != null ? agg.getAverageRating() : null;
        long reviewCount = agg != null ? agg.getReviewCount() : 0L;
        return toResponse(p, averageRating, reviewCount);
    }

    private ProductResponse toResponse(Product p, Double averageRating, long reviewCount) {
        List<String> gallery = new ArrayList<>(p.getGalleryImages());
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getSku(),
                p.getPrice(), p.getCompareAtPrice(), p.getStockQuantity(), p.getImageUrl(),
                gallery, p.getBrand(), p.getUnit(), p.getBadge(), p.isFeatured(),
                p.getStatus().name(), p.getCategory().getName(), p.getCategory().getId(),
                averageRating, reviewCount);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
