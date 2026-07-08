package com.p5store.service.impl;

import com.p5store.domain.Product;
import com.p5store.domain.Review;
import com.p5store.domain.User;
import com.p5store.dto.request.ReviewRequest;
import com.p5store.dto.response.RatingSummaryResponse;
import com.p5store.dto.response.ReviewResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.ReviewRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(Long productId, ReviewRequest req) {
        if (reviewRepository.existsByUserIdAndProductId(req.userId(), productId)) {
            throw new BusinessException("You have already reviewed this product");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.userId()));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(req.rating());
        review.setTitle(req.title());
        review.setBody(req.body());
        review.setApproved(true);

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long productId) {
        Double average = reviewRepository.averageRatingByProductId(productId);
        long count = reviewRepository.countByProductIdAndApprovedTrue(productId);
        return new RatingSummaryResponse(average, count);
    }

    private ReviewResponse toResponse(Review r) {
        String reviewerName = r.getUser().getFirstName() + " " + r.getUser().getLastName().charAt(0) + ".";
        return new ReviewResponse(r.getId(), r.getRating(), r.getTitle(), r.getBody(), reviewerName, r.getCreatedAt());
    }
}
