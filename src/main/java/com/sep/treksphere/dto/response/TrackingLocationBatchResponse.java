package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TrackingLocationBatchResponse {
    private List<UUID> acceptedSampleIds;
    private List<UUID> duplicateSampleIds;
    private List<RejectedSample> rejectedSamples;

    @Data
    @Builder
    public static class RejectedSample {
        private UUID sampleId;
        private String code;
        private String message;
    }
}
