package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.chat.ConversationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCreateRequest {

    @NotNull(message = MessageConstant.CONVERSATION_TYPE_REQUIRED)
    @Schema(description = "Loại cuộc hội thoại", example = "DIRECT")
    private ConversationType conversationType;

    @Schema(description = "Tên phòng chat nhóm; bắt buộc khi conversationType là GROUP")
    private String title;

    @NotEmpty(message = MessageConstant.CONVERSATION_PARTICIPANTS_REQUIRED)
    @Schema(description = "Danh sách người tham gia, không bao gồm người tạo")
    private List<UUID> participantIds;

    @Schema(description = "ID của Matching Group nếu tạo chat từ nhóm ghép")
    private UUID matchingGroupId;
}
