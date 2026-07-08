package com.p5store.controller;

import com.p5store.dto.request.DiscountPreviewRequest;
import com.p5store.dto.request.DiscountRequest;
import com.p5store.dto.response.DiscountPreviewResponse;
import com.p5store.dto.response.DiscountResponse;
import com.p5store.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/discounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    public Page<DiscountResponse> getAll(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return discountService.getAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(@Valid @RequestBody DiscountRequest request) {
        return discountService.create(request);
    }

    @PutMapping("/{id}")
    public DiscountResponse update(@PathVariable UUID id, @Valid @RequestBody DiscountRequest request) {
        return discountService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public DiscountResponse setActive(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return discountService.setActive(id, active);
    }

    // Overrides the class-level ADMIN restriction — any logged-in customer
    // needs to be able to check a promo code before checkout.
    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public DiscountPreviewResponse validate(@Valid @RequestBody DiscountPreviewRequest request) {
        return discountService.preview(request.code(), request.subtotal());
    }
}
