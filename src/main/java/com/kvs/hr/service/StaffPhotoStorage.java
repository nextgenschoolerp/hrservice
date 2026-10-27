package com.kvs.hr.service;

import com.kvs.core.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Local file vault for staff photos: {base-path}/{tenant}/{staffId}/{random}.{ext}.
 * Checks the size, the declared type and the file signature before anything is written.
 */
@Component
@Slf4j
public class StaffPhotoStorage {

    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final Path basePath;

    public StaffPhotoStorage(@Value("${hr.photo-vault.base-path:./hr-vault}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    /** Stores a JPEG or PNG photo and returns its path relative to the vault. */
    public String store(String tenantId, UUID staffId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded photo must not be empty");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BadRequestException("Photo exceeds the maximum size of 5 MB");
        }
        String contentType = file.getContentType();
        String extension = "image/jpeg".equals(contentType) ? ".jpg" : "image/png".equals(contentType) ? ".png" : null;
        if (extension == null) {
            throw new BadRequestException("Unsupported file type " + contentType + ". Allowed types: image/jpeg, image/png");
        }
        try {
            byte[] head = file.getInputStream().readNBytes(8);
            if (!matchesSignature(head, extension)) {
                throw new BadRequestException("File content does not match the declared type " + contentType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the photo", e);
        }
        String tenantSegment = tenantId.replaceAll("[^A-Za-z0-9_-]", "_");
        Path relative = Path.of(tenantSegment, staffId.toString(), UUID.randomUUID() + extension);
        Path target = basePath.resolve(relative).normalize();
        if (!target.startsWith(basePath)) {
            throw new BadRequestException("Invalid file storage path");
        }
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relative.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the photo", e);
        }
    }

    /** Reads a stored photo; null when it is missing on disk. */
    public byte[] read(String relativePath) {
        Path target = basePath.resolve(relativePath).normalize();
        if (!target.startsWith(basePath) || !Files.exists(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the photo", e);
        }
    }

    /** Removes a photo that was replaced; a missing file is not an error. */
    public void delete(String relativePath) {
        Path target = basePath.resolve(relativePath).normalize();
        if (!target.startsWith(basePath)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete the replaced staff photo {}", relativePath, e);
        }
    }

    static String contentTypeFor(String relativePath) {
        return relativePath.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private static boolean matchesSignature(byte[] head, String extension) {
        if (".jpg".equals(extension)) {
            return head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        }
        return Arrays.equals(Arrays.copyOf(head, Math.min(8, head.length)),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
    }
}
