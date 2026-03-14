package com.asiandoor.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadDir;

    public ProductImageStorageService(@Value("${app.upload.products-dir:uploads/products}") String productsDir) {
        this.uploadDir = Paths.get(productsDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a valid image file.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (extension.isBlank() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Allowed image formats: jpg, jpeg, png, webp, gif.");
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path destination = uploadDir.resolve(filename).normalize();

            if (!destination.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid file path.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/products/" + filename;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to save image file right now. Please try again.");
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(index + 1).toLowerCase();
    }
}
