package com.sep.treksphere.tour.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourBehaviorEventBatchRequest {

    @Valid
    @NotEmpty(message = "Danh sách hành vi không được để trống")
    @Size(max = 50, message = "Mỗi lần chỉ được gửi tối đa 50 hành vi")
    private List<TourBehaviorEventItemRequest> events;
}
