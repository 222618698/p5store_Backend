package com.p5store.service.impl;

import com.p5store.exception.BusinessException;
import com.p5store.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

@Service
public class R2FileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Client r2Client;

    public R2FileStorageService(@Lazy S3Client r2Client) {
        this.r2Client = r2Client;
    }

    @Value("${r2.bucket-name:}")
    private String bucketName;

    @Value("${r2.public-url:}")
    private String publicUrl;

    @Override
    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("Unsupported file type: " + contentType
                    + ". Allowed: JPG, PNG, WEBP");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String key = UUID.randomUUID() + extension;

        try {
            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return publicUrl.replaceAll("/$", "") + "/" + key;
    }
}
