package com.p5store.controller;

import com.p5store.dto.request.ChangePasswordRequest;
import com.p5store.dto.request.UpdateProfileRequest;
import com.p5store.dto.response.UserResponse;
import com.p5store.service.FileStorageService;
import com.p5store.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @GetMapping("/v1/users/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @PutMapping("/v1/users/{userId}")
    public UserResponse updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PostMapping("/v1/users/{userId}/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
    }

    @PostMapping("/v1/users/{userId}/avatar")
    public Map<String, String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        return Map.of("url", fileStorageService.upload(file));
    }
}
