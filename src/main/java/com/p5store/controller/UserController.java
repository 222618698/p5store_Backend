package com.p5store.controller;

import com.p5store.dto.request.ChangePasswordRequest;
import com.p5store.dto.request.UpdateProfileRequest;
import com.p5store.dto.response.UserResponse;
import com.p5store.exception.BusinessException;
import com.p5store.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserService userService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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
    public Map<String, String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("Unsupported file type: " + contentType
                    + ". Allowed: JPG, PNG, WEBP");
        }

        Path dir = Path.of(uploadDir);
        Files.createDirectories(dir);

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;
        file.transferTo(dir.resolve(filename));

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();
        return Map.of("url", url);
    }
}
