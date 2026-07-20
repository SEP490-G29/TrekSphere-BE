package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.PorterProfileFilterRequest;
import com.sep.treksphere.dto.request.PorterProfileRequest;
import com.sep.treksphere.dto.request.UpdatePorterProfileRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.PorterProfileDto;

import java.util.List;
import java.util.UUID;

public interface PorterProfileService {
    PorterProfileDto createPorter(String email, PorterProfileRequest request);
    PorterProfileDto updatePorter(String email, UUID porterId, UpdatePorterProfileRequest request);
    void deletePorter(String email, UUID porterId);
    PaginationResponse<PorterProfileDto> getPorters(String email, PorterProfileFilterRequest request);
    List<PorterProfileDto> getAllPortersList(String email, PorterProfileFilterRequest request);
}
