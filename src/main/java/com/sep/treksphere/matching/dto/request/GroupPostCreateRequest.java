package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.GroupPostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo bài đăng trong không gian làm việc nhóm")
public class GroupPostCreateRequest {

    @Schema(description = "Loại bài đăng", example = "DISCUSSION")
    private GroupPostType postType;

    @Schema(description = "Ghim bài viết (chỉ Leader)", example = "false")
    private Boolean isPinned;

    @Size(max = 200, message = MessageConstant.POST_TITLE_MAX_LENGTH)
    @Schema(description = "Tiêu đề bài đăng (tùy chọn)", example = "Cập nhật tình hình thời tiết đỉnh Tà Xùa")
    private String title;

    @NotBlank(message = MessageConstant.POST_CONTENT_REQUIRED)
    @Schema(description = "Nội dung bài đăng", example = "Cuối tuần này dự báo có nắng và mây đẹp, mọi người chuẩn bị đủ đồ ấm nhé!")
    private String content;

    @Schema(description = "Danh sách URL ảnh đính kèm")
    private List<String> imageUrls;
}
