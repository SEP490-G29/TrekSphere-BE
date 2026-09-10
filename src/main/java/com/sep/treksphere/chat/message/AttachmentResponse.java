package com.sep.treksphere.chat.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private String name;
    private String mimeType;
    private Long sizeBytes;
    private String downloadUrl;
}
