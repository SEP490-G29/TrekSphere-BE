package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourImageResponse {

    private String imageId;
    private String imageUrl;
    private Integer sortOrder;
    private String caption;
}
