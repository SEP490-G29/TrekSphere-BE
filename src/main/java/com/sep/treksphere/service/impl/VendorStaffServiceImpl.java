package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorStaffResponse;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorStaffMapper;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.VendorStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorStaffServiceImpl implements VendorStaffService {

    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final VendorStaffMapper vendorStaffMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorStaffResponse> getMyVendorStaff(String managerEmail, BaseFilterRequest request) {
        Vendor vendor = vendorRepository.findByManager_Email(managerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        Page<VendorStaff> staffPage = vendorStaffRepository.findByVendorIdAndKeyword(
                vendor.getVendorId(),
                request.getKeyword(),
                request.getPageable()
        );

        List<VendorStaffResponse> responses = staffPage.getContent().stream()
                .map(vendorStaffMapper::toVendorStaffResponse)
                .toList();

        return PaginationResponse.<VendorStaffResponse>builder()
                .content(responses)
                .pageNumber(staffPage.getNumber())
                .pageSize(staffPage.getSize())
                .totalElements(staffPage.getTotalElements())
                .totalPages(staffPage.getTotalPages())
                .last(staffPage.isLast())
                .build();
    }
}
