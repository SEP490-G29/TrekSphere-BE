package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.RejectTourRequest;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TourService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorTourControllerTest {

    @Mock private TourService tourService;
    @Mock private CustomUserDetails userDetails;

    @InjectMocks
    private VendorTourController controller;

    @Test
    void approveTour_ReturnsApprovedTour() {
        UUID tourId = UUID.randomUUID();
        TourDetailResponse data = TourDetailResponse.builder()
                .tourId(tourId.toString())
                .status(TourStatus.APPROVED)
                .build();
        when(userDetails.getUsername()).thenReturn("manager@treksphere.test");
        when(tourService.approveTour(userDetails.getUsername(), tourId)).thenReturn(data);

        var response = controller.approveTour(userDetails, tourId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(MessageConstant.TOUR_APPROVED_SUCCESSFULLY);
        assertThat(response.getBody().getData()).isSameAs(data);
        verify(tourService).approveTour(userDetails.getUsername(), tourId);
    }

    @Test
    void rejectTour_ForwardsReasonAndReturnsRejectedTour() {
        UUID tourId = UUID.randomUUID();
        RejectTourRequest request = new RejectTourRequest();
        request.setReason("Thiếu kế hoạch an toàn");
        TourDetailResponse data = TourDetailResponse.builder()
                .tourId(tourId.toString())
                .status(TourStatus.REJECTED)
                .rejectionReason(request.getReason())
                .build();
        when(userDetails.getUsername()).thenReturn("manager@treksphere.test");
        when(tourService.rejectTour(userDetails.getUsername(), tourId, request.getReason())).thenReturn(data);

        var response = controller.rejectTour(userDetails, tourId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(MessageConstant.TOUR_REJECTED_SUCCESSFULLY);
        assertThat(response.getBody().getData()).isSameAs(data);
        verify(tourService).rejectTour(userDetails.getUsername(), tourId, request.getReason());
    }
}
