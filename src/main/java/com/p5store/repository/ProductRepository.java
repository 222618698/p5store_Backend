package com.p5store.repository;

import com.p5store.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.p5store.enums.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);
    long countByCategory_Id(Long categoryId);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = :status",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.featured = true AND p.status = :status")
    List<Product> findByFeaturedTrueAndStatus(ProductStatus status);

    // Categories are 2 levels deep (top-level + subcategories) and products
    // are tagged to the subcategory, not the top-level parent — so browsing a
    // top-level category must also include its direct children's products.
    // JOIN FETCH avoids an N+1 lazy-load of category per row (toResponse()
    // reads product.getCategory().getName()) — severe once result sets are
    // thousands of rows rather than a handful of demo products.
    @Query("SELECT p FROM Product p JOIN FETCH p.category c WHERE p.status = :status " +
           "AND (c.id = :categoryId OR c.parent.id = :categoryId)")
    List<Product> findByCategoryIdOrParentIdAndStatus(Long categoryId, ProductStatus status);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = 'ACTIVE' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Product> search(String q);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = 'ACTIVE' AND p.price BETWEEN :min AND :max")
    List<Product> findByPriceRange(BigDecimal min, BigDecimal max);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Product> findNewArrivals(Pageable pageable);

    // Rolls subcategory product counts up to their top-level ancestor (or the
    // category itself if it has no parent), so the storefront sidebar can show
    // an accurate count per top-level category without fetching every product.
    @Query("SELECT COALESCE(c.parent.id, c.id) AS categoryId, COUNT(p) AS count " +
           "FROM Product p JOIN p.category c WHERE p.status = 'ACTIVE' " +
           "GROUP BY COALESCE(c.parent.id, c.id)")
    List<CategoryProductCount> countByTopLevelCategory();

    interface CategoryProductCount {
        Long getCategoryId();
        Long getCount();
    }
}
