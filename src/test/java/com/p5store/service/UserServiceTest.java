package com.p5store.service;

import com.p5store.domain.Cart;
import com.p5store.domain.User;
import com.p5store.dto.request.LoginRequest;
import com.p5store.dto.request.RegisterRequest;
import com.p5store.dto.response.AuthResponse;
import com.p5store.dto.response.UserResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.impl.JwtService;
import com.p5store.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks UserServiceImpl userService;

    User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("$2a$encoded");
        user.setActive(true);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest("John", "Doe", "john@example.com", "pass123", null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(cartRepository.save(any())).thenReturn(new Cart());
        when(jwtService.generateToken(any(), any())).thenReturn("token123");

        AuthResponse resp = userService.register(req);
        assertThat(resp.token()).isEqualTo("token123");
        verify(cartRepository).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest("John", "Doe", "john@example.com", "pass123", null);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "$2a$encoded")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        AuthResponse resp = userService.login(new LoginRequest("john@example.com", "pass123"));
        assertThat(resp.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("john@example.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_inactiveAccount_throws() {
        user.setActive(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new LoginRequest("john@example.com", "pass123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void getUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserResponse resp = userService.getUser(1L);
        assertThat(resp.email()).isEqualTo("john@example.com");
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
