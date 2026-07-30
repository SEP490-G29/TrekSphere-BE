package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupCreateRequest {

    @NotNull(message = "Mã tour không được để trống")
    private UUID tourId;

    @NotBlank(message = "Tên nhóm ghép không được để trống")
    @Size(min = 3, max = 100, message = "Tên nhóm ghép phải từ 3 đến 100 ký tự")
    private String groupName;

    private String description;

    @NotNull(message = "Số lượng thành viên tối đa không được để trống")
    @Min(value = 2, message = "Số lượng thành viên tối đa phải từ 2 trở lên")
    @Max(value = 100, message = "Số lượng thành viên tối đa không vượt quá 100 người")
    private Integer maxSize;

    @NotNull(message = "Ngày đi mong muốn không được để trống")
    private LocalDate targetDate;

    @NotNull(message = "Hạn chót ghép nhóm không được để trống")
    private LocalDateTime matchingDeadline;
}
