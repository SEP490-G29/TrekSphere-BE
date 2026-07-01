package com.sep.treksphere.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sep.treksphere.enums.common.ErrorCode;
import com.sep.treksphere.exception.BusinessException;
import com.sep.treksphere.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final Cloudinary cloudinary;


    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;


    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            // Ảnh
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        try {
            String resourceType = resolveResourceType(file.getContentType());

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType
                    ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("File uploaded successfully: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file, folder));
        }
        return urls;
    }

    @Override
    public void deleteFile(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete result for [{}]: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILED);
        }
    }


    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size {} exceeds limit of {} bytes", file.getSize(), MAX_FILE_SIZE);
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("Invalid file format: {}", contentType);
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }


    private String resolveResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return "image";
        }
        return "raw";
    }
}
