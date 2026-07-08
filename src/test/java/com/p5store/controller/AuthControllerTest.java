package com.p5store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.dto.request.LoginRequest;
import com.p5store.dto.request.RegisterRequest;
import com.p5store.dto.response.AuthResponse;
import com.p5store.config.JwtAuthFilter;
import com.p5store.service.UserService;
import com.p5store.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserService userService;

    @Test
    @WithMockUser
    void register_returns201() throws Exception {
        given(userService.register(any())).willReturn(new AuthResponse("token", 1L, "john@example.com", "CUSTOMER"));

        mockMvc.perform(post("/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "john@example.com", "pass123", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    @WithMockUser
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("John", "Doe", "not-an-email", "pass123", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void login_returns200() throws Exception {
        given(userService.login(any())).willReturn(new AuthResponse("token", 1L, "john@example.com", "CUSTOMER"));

        mockMvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    @WithMockUser
    void login_badCredentials_returns400() throws Exception {
        given(userService.login(any())).willThrow(new BusinessException("Invalid credentials"));

        mockMvc.perform(post("/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("john@example.com", "wrong"))))
                .andExpect(status().isBadRequest());
    }
}
