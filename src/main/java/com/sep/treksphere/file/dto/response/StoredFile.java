package com.sep.treksphere.file.dto.response;

public record StoredFile(
        String storageId,
        String url,
        String originalName,
        String mimeType,
        long sizeBytes
) {
}
