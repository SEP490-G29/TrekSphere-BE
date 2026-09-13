package com.sep.treksphere.matching.dto.request;

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
public class GroupPostCreateRequest {

    @Size(max = 200, message = "Tiêu đề bài đăng không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung bài đăng không được để trống")
    private String content;

    private List<String> imageUrls;
}
