package com.sep.treksphere.matching.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPostUpdateRequest {

    @Size(max = 200, message = "Tiêu đề bài đăng không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung bài đăng không được để trống")
    private String content;

    private List<String> imageUrls;
}
