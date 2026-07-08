package com.p5store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.config.JwtAuthFilter;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProductService productService;

    ProductResponse sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new ProductResponse(1L, "Laptop", "Desc", "LAP-001",
                new BigDecimal("999.00"), null, 10, null, null, null, null, null, false, "ACTIVE", "Electronics", 1L,
                null, 0L);
    }

    @Test
    void getAll_noAuth_returns200() throws Exception {
        given(productService.getAll(any())).willReturn(new PageImpl<>(List.of(sampleProduct), PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    @Test
    void getById_noAuth_returns200() throws Exception {
        given(productService.getById(1L)).willReturn(sampleProduct);

        mockMvc.perform(get("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LAP-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        given(productService.getById(99L)).willThrow(new ResourceNotFoundException("Product not found: 99"));

        mockMvc.perform(get("/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/v1/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void delete_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/v1/products/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
