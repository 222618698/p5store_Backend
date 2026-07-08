package com.p5store.service;

import com.p5store.domain.Category;
import com.p5store.enums.ProductStatus;
import com.p5store.domain.Product;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.ReviewRepository;
import com.p5store.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks ProductServiceImpl productService;

    Category category;
    Product product;
    ProductRequest request;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(1L);
        product.setName("Phone");
        product.setSku("SKU-001");
        product.setPrice(new BigDecimal("999.00"));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setStatus(ProductStatus.ACTIVE);

        request = new ProductRequest("Phone", "Desc", "SKU-001",
                new BigDecimal("999.00"), null, 10, null, null, null, null, null, false, null, 1L);
    }

    @Test
    void create_success() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenReturn(product);

        ProductResponse resp = productService.create(request);

        assertThat(resp.name()).isEqualTo("Phone");
        verify(productRepository).save(any());
    }

    @Test
    void create_duplicateSku_throws() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    void create_categoryNotFound_throws() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_zeroStock_statusIsOutOfStock() {
        ProductRequest zeroStock = new ProductRequest("Phone", null, "SKU-002",
                new BigDecimal("999.00"), null, 0, null, null, null, null, null, false, null, 1L);
        Product outOfStock = new Product();
        outOfStock.setId(2L);
        outOfStock.setSku("SKU-002");
        outOfStock.setStockQuantity(0);
        outOfStock.setStatus(ProductStatus.OUT_OF_STOCK);
        outOfStock.setCategory(category);

        when(productRepository.existsBySku("SKU-002")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenReturn(outOfStock);

        ProductResponse resp = productService.create(zeroStock);
        assertThat(resp.status()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void getById_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThat(productService.getById(1L).sku()).isEqualTo("SKU-001");
    }

    @Test
    void getById_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_success() {
        ProductRequest updateReq = new ProductRequest("Updated", "D", "SKU-001",
                new BigDecimal("799.00"), null, 5, null, null, null, null, null, true, null, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse resp = productService.update(1L, updateReq);
        assertThat(resp.name()).isEqualTo("Updated");
    }

    @Test
    void delete_softDeletes() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.delete(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        verify(productRepository).save(product);
    }

    @Test
    void getAll_returnsMappedPage() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByStatus(eq(ProductStatus.ACTIVE), any())).thenReturn(page);

        Page<ProductResponse> result = productService.getAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void search_returnsResults() {
        when(productRepository.search("phone")).thenReturn(List.of(product));
        assertThat(productService.search("phone")).hasSize(1);
    }
}
