package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.mapper.VendorMapper;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorResponse> getVendors(BaseFilterRequest request) {
        Page<Vendor> vendorsPage = vendorRepository.findByKeyword(
                request.getKeyword(),
                request.getPageable()
        );

        List<VendorResponse> responses = vendorsPage.getContent().stream()
                .map(vendorMapper::toVendorResponse)
                .toList();

        return PaginationResponse.<VendorResponse>builder()
                .content(responses)
                .pageNumber(vendorsPage.getNumber())
                .pageSize(vendorsPage.getSize())
                .totalElements(vendorsPage.getTotalElements())
                .totalPages(vendorsPage.getTotalPages())
                .last(vendorsPage.isLast())
                .build();
    }
}
