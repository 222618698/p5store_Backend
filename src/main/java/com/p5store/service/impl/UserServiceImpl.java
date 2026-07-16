package com.p5store.service.impl;

import com.p5store.domain.Cart;
import com.p5store.domain.PasswordResetToken;
import com.p5store.domain.User;
import com.p5store.dto.request.ChangePasswordRequest;
import com.p5store.dto.request.ForgotPasswordRequest;
import com.p5store.dto.request.LoginRequest;
import com.p5store.dto.request.RegisterRequest;
import com.p5store.dto.request.ResetPasswordRequest;
import com.p5store.dto.request.UpdateProfileRequest;
import com.p5store.dto.response.AuthResponse;
import com.p5store.dto.response.UserResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.PasswordResetTokenRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.EmailService;
import com.p5store.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
        // Always succeeds from the caller's perspective, whether or not the
        // email exists — avoids leaking which addresses are registered.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(req.token())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset link"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BusinessException("Invalid or expired reset link");

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        resetToken.setUsed(true);

        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(),
                u.getEmail(), u.getPhone(), u.getRole().name(),
                u.getAvatarUrl(), u.isNewsletterOptIn(), u.isOffersOptIn(), u.isSmsOptIn());
    }
}
