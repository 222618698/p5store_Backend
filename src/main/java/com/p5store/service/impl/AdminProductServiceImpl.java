package com.p5store.service.impl;

import com.p5store.config.ProductMapper;
import com.p5store.domain.Category;
import com.p5store.domain.Product;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.enums.ProductStatus;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    // ── CREATE ──────────────────────────────────────────────────
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // 1. Guard duplicate SKU
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }

        // 2. Resolve category
        Category category = findCategory(request.categoryId());

        // 3. Build domain object via the mapper, then attach category
        Product product = productMapper.toEntity(request);
        product.setCategory(category);

        // 4. Persist
        Product saved = productRepository.save(product);

        log.info("Admin created product '{}' (SKU: {}) in category '{}'",
                saved.getName(), saved.getSku(), category.getName());

        return productMapper.toResponse(saved);
    }

    // ── UPDATE ──────────────────────────────────────────────────
    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = findProduct(id);
        Category category = findCategory(request.categoryId());

        productMapper.updateEntityFromRequest(request, existing);
        existing.setCategory(category);

        Product saved = productRepository.save(existing);
        log.info("Admin updated product '{}' (id: {})", saved.getName(), id);
        return productMapper.toResponse(saved);
    }

    // ── SOFT DELETE ─────────────────────────────────────────────
    @Override
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        log.info("Admin soft-deleted product '{}' (id: {})", product.getName(), id);
    }

    // ── LIST ALL (including inactive) ────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
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
