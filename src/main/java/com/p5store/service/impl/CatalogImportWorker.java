package com.p5store.service.impl;

import com.p5store.domain.Category;
import com.p5store.domain.Product;
import com.p5store.dto.request.CatalogImportCategory;
import com.p5store.dto.request.CatalogImportProduct;
import com.p5store.enums.ProductStatus;
import com.p5store.repository.CartItemRepository;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.OrderItemRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transactional building blocks for {@link CatalogImportService}, split into
 * a separate bean so each method's @Transactional boundary is honored — calls
 * from CatalogImportService go through this bean's proxy rather than being
 * internal self-invocations (which Spring's proxy-based @Transactional can't
 * intercept).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogImportWorker {

    public static final int BATCH_SIZE = 500;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void wipeExistingCatalog() {
        log.info("Wiping existing catalog (products, categories, reviews, cart items)...");
        reviewRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        orderItemRepository.detachAllProducts();
        categoryRepository.detachAllParents();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Transactional
    public Map<String, Long> createCategories(List<CatalogImportCategory> categories) {
        // Category.name is globally unique, but the source taxonomy can define
        // the same name twice (e.g. "Bags" both as its own top-level category
        // and as a distinct subcategory under "Fashion"). First definition
        // wins; later same-name entries just reuse the id already created.
        Map<String, Long> idByName = new HashMap<>();
        for (CatalogImportCategory c : categories) {
            if (c.parentName() != null) continue;
            if (idByName.containsKey(c.name())) continue;
            Category cat = new Category();
            cat.setName(c.name());
            idByName.put(c.name(), categoryRepository.save(cat).getId());
        }
        for (CatalogImportCategory c : categories) {
            if (c.parentName() == null) continue;
            if (idByName.containsKey(c.name())) continue;
            Long parentId = idByName.get(c.parentName());
            if (parentId == null) {
                log.warn("Skipping subcategory '{}': parent '{}' not found", c.name(), c.parentName());
                continue;
            }
            Category cat = new Category();
            cat.setName(c.name());
            cat.setParent(entityManager.getReference(Category.class, parentId));
            idByName.put(c.name(), categoryRepository.save(cat).getId());
        }
        return idByName;
    }

    /**
     * Imports one batch (a few hundred products) in its own transaction, so a
     * crash partway through the full ~30k-row import (this ran on a
     * memory-constrained free-tier instance and did crash mid-import before)
     * leaves already-committed batches intact instead of rolling back
     * everything imported so far.
     */
    @Transactional
    public int importBatch(List<CatalogImportProduct> batch, Map<String, Long> categoryIdByName) {
        int count = 0;
        for (CatalogImportProduct p : batch) {
            Long categoryId = categoryIdByName.get(p.categoryName());
            if (categoryId == null) {
                log.warn("Skipping product '{}': category '{}' not found", p.name(), p.categoryName());
                continue;
            }

            Product product = new Product();
            product.setName(sanitize(p.name()));
            product.setDescription(sanitize(p.description()));
            product.setSku(sanitize(p.sku()));
            product.setPrice(p.price());
            product.setCompareAtPrice(p.compareAtPrice());
            int qty = p.stockQuantity() != null ? p.stockQuantity() : 0;
            product.setStockQuantity(qty);
            product.setImageUrl(sanitize(p.imageUrl()));
            product.setGalleryImages(p.galleryImages() != null
                    ? p.galleryImages().stream().map(this::sanitize).collect(Collectors.toCollection(ArrayList::new))
                    : new ArrayList<>());
            product.setCategory(entityManager.getReference(Category.class, categoryId));
            product.setStatus(qty > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK);

            productRepository.save(product);
            count++;
        }
        entityManager.flush();
        return count;
    }

    // Postgres text columns reject the NUL byte outright; the source
    // WordPress export has stray null bytes in some scraped fields, which
    // previously killed an entire batch's transaction and halted the import.
    private String sanitize(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != 0) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
