package com.p5store.service.impl;

import com.p5store.domain.Discount;
import com.p5store.dto.request.DiscountRequest;
import com.p5store.dto.response.DiscountPreviewResponse;
import com.p5store.dto.response.DiscountResponse;
import com.p5store.enums.DiscountType;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.DiscountRepository;
import com.p5store.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public DiscountResponse create(DiscountRequest req) {
        if (discountRepository.existsByCodeIgnoreCase(req.code())) {
            throw new BusinessException("Promo code already exists: " + req.code());
        }
        Discount discount = new Discount();
        applyRequest(discount, req);
        return toResponse(discountRepository.save(discount));
    }

    @Override
    public DiscountResponse update(UUID id, DiscountRequest req) {
        Discount discount = findDiscount(id);
        if (!discount.getCode().equalsIgnoreCase(req.code())
                && discountRepository.existsByCodeIgnoreCase(req.code())) {
            throw new BusinessException("Promo code already exists: " + req.code());
        }
        applyRequest(discount, req);
        return toResponse(discountRepository.save(discount));
    }

    @Override
    public DiscountResponse setActive(UUID id, boolean active) {
        Discount discount = findDiscount(id);
        discount.setActive(active);
        return toResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountResponse> getAll(Pageable pageable) {
        return discountRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountPreviewResponse preview(String code, BigDecimal subtotal) {
        Optional<Discount> found = discountRepository.findByCodeIgnoreCase(code);
        if (found.isEmpty()) {
            return new DiscountPreviewResponse(false, BigDecimal.ZERO, "Invalid promo code");
        }
        String invalidReason = invalidReason(found.get());
        if (invalidReason != null) {
            return new DiscountPreviewResponse(false, BigDecimal.ZERO, invalidReason);
        }
        BigDecimal amount = computeAmount(found.get(), subtotal);
        return new DiscountPreviewResponse(true, amount, "Promo code applied");
    }

    @Override
    public BigDecimal apply(String code, BigDecimal subtotal) {
        Discount discount = discountRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Invalid promo code: " + code));
        String invalidReason = invalidReason(discount);
        if (invalidReason != null) {
            throw new BusinessException(invalidReason);
        }
        BigDecimal amount = computeAmount(discount, subtotal);
        discount.setUsageCount(discount.getUsageCount() + 1);
        discountRepository.save(discount);
        return amount;
    }

    private String invalidReason(Discount d) {
        if (!d.isActive()) return "This promo code is no longer active";
        if (d.getValidTo() != null && d.getValidTo().isBefore(LocalDateTime.now())) {
            return "This promo code has expired";
        }
        if (d.getUsageLimit() != null && d.getUsageCount() >= d.getUsageLimit()) {
            return "This promo code has reached its usage limit";
        }
        return null;
    }

    private BigDecimal computeAmount(Discount d, BigDecimal subtotal) {
        BigDecimal amount = d.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(d.getValue()).divide(new BigDecimal("100"))
                : d.getValue();
        return amount.min(subtotal);
    }

    private void applyRequest(Discount discount, DiscountRequest req) {
        discount.setCode(req.code().toUpperCase());
        discount.setDiscountType(req.discountType());
        discount.setValue(req.value());
        discount.setUsageLimit(req.usageLimit());
        discount.setValidTo(req.validTo());
        if (req.active() != null) {
            discount.setActive(req.active());
        }
    }

    private DiscountResponse toResponse(Discount d) {
        return new DiscountResponse(d.getId(), d.getCode(), d.getDiscountType().name(),
                d.getValue(), d.getUsageLimit(), d.getUsageCount(), d.getValidTo(), d.isActive());
    }

    private Discount findDiscount(UUID id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found: " + id));
    }
}
