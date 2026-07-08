package com.p5store.controller;

import com.p5store.dto.request.ReviewRequest;
import com.p5store.dto.response.RatingSummaryResponse;
import com.p5store.dto.response.ReviewResponse;
import com.p5store.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewResponse> getReviews(@PathVariable Long productId) {
        return reviewService.getProductReviews(productId);
    }

    @GetMapping("/summary")
    public RatingSummaryResponse getSummary(@PathVariable Long productId) {
        return reviewService.getRatingSummary(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(productId, request);
    }
}
