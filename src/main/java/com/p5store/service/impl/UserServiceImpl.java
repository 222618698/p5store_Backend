package com.p5store.service.impl;

import com.p5store.domain.Cart;
import com.p5store.domain.User;
import com.p5store.dto.request.ChangePasswordRequest;
import com.p5store.dto.request.LoginRequest;
import com.p5store.dto.request.RegisterRequest;
import com.p5store.dto.request.UpdateProfileRequest;
import com.p5store.dto.response.AuthResponse;
import com.p5store.dto.response.UserResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new BusinessException("Email already registered: " + req.email());

        User user = new User();
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));
        if (!user.isActive())
            throw new BusinessException("Account is disabled");
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()))
            throw new BusinessException("Invalid credentials");

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        userRepository.findByEmail(req.email()).ifPresent(existing -> {
            if (!existing.getId().equals(userId))
                throw new BusinessException("Email already registered: " + req.email());
        });

        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        if (req.avatarUrl() != null) user.setAvatarUrl(req.avatarUrl());
        if (req.newsletterOptIn() != null) user.setNewsletterOptIn(req.newsletterOptIn());
        if (req.offersOptIn() != null) user.setOffersOptIn(req.offersOptIn());
        if (req.smsOptIn() != null) user.setSmsOptIn(req.smsOptIn());

        return toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash()))
            throw new BusinessException("Current password is incorrect");

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(),
                u.getEmail(), u.getPhone(), u.getRole().name(),
                u.getAvatarUrl(), u.isNewsletterOptIn(), u.isOffersOptIn(), u.isSmsOptIn());
    }
}
