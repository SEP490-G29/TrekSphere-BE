package com.sep.treksphere.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final Cloudinary cloudinary;


    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;


    public String uploadFile(MultipartFile file, String folder) {
        return upload(file, folder, UploadPolicy.IMAGE).url();
    }

    public StoredFile upload(MultipartFile file, String folder, UploadPolicy policy) {
        byte[] bytes = validateFile(file, policy);

        try {
            String resourceType = resolveResourceType(file.getContentType());

            Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType
                    ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = String.valueOf(uploadResult.get("public_id"));
            log.info("File uploaded successfully to folder {} with storage id {}", folder, publicId);
            return new StoredFile(
                    publicId,
                    secureUrl,
                    sanitizeFilename(file.getOriginalFilename()),
                    file.getContentType(),
                    file.getSize()
            );

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    public List<String> uploadFiles(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file, folder));
        }
        return urls;
    }

    public void deleteFile(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete result for [{}]: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    public void deleteFileBestEffort(String publicId, String mimeType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resolveResourceType(mimeType)));
        } catch (IOException | RuntimeException ex) {
            log.warn("Unable to clean up uploaded file {}: {}", publicId, ex.getMessage());
        }
    }


    private byte[] validateFile(MultipartFile file, UploadPolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size {} exceeds limit of {} bytes", file.getSize(), MAX_FILE_SIZE);
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        String extension = extensionOf(file.getOriginalFilename());
        if (contentType == null
                || !policy.contentTypes().contains(contentType.toLowerCase(Locale.ROOT))
                || !policy.extensions().contains(extension)) {
            log.warn("Invalid file format: {}", contentType);
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(contentType, bytes)) {
                log.warn("File signature does not match declared content type {}", contentType);
                throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
            }
            return bytes;
        } catch (IOException ex) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }


    private String resolveResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return "image";
        }
        return "raw";
    }

    private String extensionOf(String filename) {
        String safeName = filename == null ? "" : filename.trim();
        int dot = safeName.lastIndexOf('.');
        return dot >= 0 ? safeName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sanitizeFilename(String filename) {
        String name = filename == null ? "file" : filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\r\\n\\t]", "_")
                .replaceAll("[^a-zA-Z0-9._() -]", "_");
        if (name.isBlank()) {
            return "file";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> bytes.length >= 12
                    && startsWith(bytes, 0x52, 0x49, 0x46, 0x46)
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            case "application/pdf" -> startsWith(bytes, 0x25, 0x50, 0x44, 0x46);
            case "application/msword", "application/vnd.ms-excel" ->
                    startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    startsWith(bytes, 0x50, 0x4B, 0x03, 0x04);
            case "text/plain" -> {
                boolean hasNullByte = false;
                for (byte value : bytes) {
                    if (value == 0) {
                        hasNullByte = true;
                        break;
                    }
                }
                yield !hasNullByte;
            }
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
