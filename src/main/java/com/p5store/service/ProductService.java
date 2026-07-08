package com.p5store.service;

import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
    ProductResponse getById(Long id);
    ProductResponse getBySku(String sku);
    Page<ProductResponse> getAll(Pageable pageable);
    List<ProductResponse> getFeatured();
    List<ProductResponse> getNewArrivals();
    List<ProductResponse> getByCategory(Long categoryId);
    List<ProductResponse> search(String q);
    List<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max);
}
