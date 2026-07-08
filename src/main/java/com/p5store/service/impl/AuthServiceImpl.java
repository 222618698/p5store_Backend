package com.p5store.service.impl;

import com.p5store.domain.User;
import com.p5store.dto.request.AuthDtos.*;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.repository.UserRepository;
import com.p5store.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// Not a @Service: unused duplicate of UserServiceImpl (which AuthController actually calls),
// and it requires an AuthenticationManager bean that isn't configured in SecurityConfig.
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Email already registered: " + req.email());
        }

        User user = User.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .build();

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getId().toString(), saved.getRole().name());

        return toResponse(saved, token);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException if wrong password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email())
                .orElseThrow();

        String token = jwtService.generateToken(user.getId().toString(), user.getRole().name());

        return toResponse(user, token);
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRole().name());
    }
}