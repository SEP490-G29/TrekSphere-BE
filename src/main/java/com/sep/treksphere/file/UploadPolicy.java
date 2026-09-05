package com.sep.treksphere.file;

import java.util.Set;

public enum UploadPolicy {
    IMAGE(Set.of("jpg", "jpeg", "png", "webp"), Set.of(
            "image/jpeg", "image/png", "image/webp")),
    BUSINESS_LICENSE(Set.of("jpg", "jpeg", "png", "pdf"), Set.of(
            "image/jpeg", "image/png", "application/pdf")),
    CHAT_ATTACHMENT(Set.of("jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt"), Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"));

    private final Set<String> extensions;
    private final Set<String> contentTypes;

    UploadPolicy(Set<String> extensions, Set<String> contentTypes) {
        this.extensions = extensions;
        this.contentTypes = contentTypes;
    }

    public Set<String> extensions() {
        return extensions;
    }

    public Set<String> contentTypes() {
        return contentTypes;
    }
}
