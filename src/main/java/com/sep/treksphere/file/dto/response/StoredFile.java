package com.sep.treksphere.file;

public record StoredFile(
        String storageId,
        String url,
        String originalName,
        String mimeType,
        long sizeBytes
) {
}
