package com.sep.treksphere.tour.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourBehaviorEventItemRequest {

    @NotNull(message = "Tour không được để trống")
    private UUID tourId;

    @NotNull(message = "Loại hành vi không được để trống")
    private TourBehaviorEventType eventType;

    @NotNull(message = "Nguồn hành vi không được để trống")
    private TourBehaviorSource source;

    @Size(max = 100, message = "Session ID không được vượt quá 100 ký tự")
    private String sessionId;

    @Min(value = 0, message = "Vị trí hiển thị không được âm")
    @Max(value = 1000, message = "Vị trí hiển thị không được vượt quá 1000")
    private Integer displayPosition;
}
