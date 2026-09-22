package com.sep.treksphere.chat.message;

import lombok.*;

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
