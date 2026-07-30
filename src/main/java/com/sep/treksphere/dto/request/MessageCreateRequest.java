package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {

    @NotNull(message = MessageConstant.MESSAGE_CONVERSATION_ID_REQUIRED)
    @Schema(description = "Mã cuộc hội thoại nhận tin nhắn")
    private UUID conversationId;

    @NotBlank(message = MessageConstant.MESSAGE_CONTENT_REQUIRED)
    @Schema(description = "Nội dung tin nhắn")
    private String content;
}
