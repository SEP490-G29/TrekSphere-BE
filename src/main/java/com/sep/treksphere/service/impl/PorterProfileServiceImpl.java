package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.PorterProfileFilterRequest;
import com.sep.treksphere.dto.request.PorterProfileRequest;
import com.sep.treksphere.dto.request.UpdatePorterProfileRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.PorterProfileDto;
import com.sep.treksphere.entity.PorterProfile;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.vendor.PorterStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.PorterProfileMapper;
import com.sep.treksphere.repository.PorterProfileRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.FileService;
import com.sep.treksphere.service.PorterProfileService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PorterProfileServiceImpl implements PorterProfileService {

    private final PorterProfileRepository porterProfileRepository;
    private final PorterProfileMapper porterProfileMapper;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final FileService fileService;

    private Vendor getCurrentVendor(String email) {
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isManager = user.getRoles().stream().anyMatch(r -> "VENDOR_MANAGER".equals(r.getRoleName()));
        boolean isStaff = user.getRoles().stream().anyMatch(r -> "VENDOR_STAFF".equals(r.getRoleName()));

        if (isManager) {
            return vendorRepository.findByManager_UserId(user.getUserId())
                    .filter(v -> !v.getIsDeleted())
                    .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
        } else if (isStaff) {
            return vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(user.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.VENDOR_STAFF_NOT_FOUND))
                    .getVendor();
        } else {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    @Transactional
    public PorterProfileDto createPorter(String email, PorterProfileRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        
        PorterProfile porter = porterProfileMapper.toEntity(request);
        porter.setVendor(currentVendor);
        porter.setJoinedDate(LocalDate.now());
        porter.setStatus(PorterStatus.ACTIVE);
        
        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            log.info("Uploading avatar for new porter");
            String avatarUrl = fileService.uploadFile(request.getAvatarFile(), "porter-avatars");
            porter.setAvatarUrl(avatarUrl);
        } else if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            porter.setAvatarUrl(request.getAvatarUrl());
        }
        
        PorterProfile savedPorter = porterProfileRepository.save(porter);
        log.info("Porter created successfully with ID: {}", savedPorter.getPorterId());
        
        return porterProfileMapper.toDto(savedPorter);
    }

    @Override
    @Transactional
    public PorterProfileDto updatePorter(String email, UUID porterId, UpdatePorterProfileRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        
        PorterProfile porter = porterProfileRepository.findById(porterId)
                .orElseThrow(() -> new AppException(ErrorCode.PORTER_NOT_FOUND));
                
        if (!porter.getVendor().getVendorId().equals(currentVendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        porterProfileMapper.updateEntity(porter, request);
        
        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            log.info("Uploading new avatar for porter ID: {}", porterId);
            String avatarUrl = fileService.uploadFile(request.getAvatarFile(), "porter-avatars");
            porter.setAvatarUrl(avatarUrl);
        } else if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            porter.setAvatarUrl(request.getAvatarUrl());
        }
        
        PorterProfile updatedPorter = porterProfileRepository.save(porter);
        log.info("Porter updated successfully with ID: {}", updatedPorter.getPorterId());
        
        return porterProfileMapper.toDto(updatedPorter);
    }

    @Override
    @Transactional
    public void deletePorter(String email, UUID porterId) {
        Vendor currentVendor = getCurrentVendor(email);
        
        PorterProfile porter = porterProfileRepository.findById(porterId)
                .orElseThrow(() -> new AppException(ErrorCode.PORTER_NOT_FOUND));
                
        if (!porter.getVendor().getVendorId().equals(currentVendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        porter.setStatus(PorterStatus.INACTIVE);
        porterProfileRepository.save(porter);
        log.info("Porter deleted (status changed to INACTIVE) successfully with ID: {}", porterId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<PorterProfileDto> getPorters(String email, PorterProfileFilterRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        String searchKeyword = request.getKeyword() == null ? "" : request.getKeyword();
        
        Page<PorterProfile> porters = porterProfileRepository.findByVendorIdAndFilters(
                currentVendor.getVendorId(), 
                searchKeyword,
                request.getStatus(),
                request.getPageable());
                
        return PaginationUtils.toPaginationResponse(porters.map(porterProfileMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PorterProfileDto> getAllPortersList(String email, PorterProfileFilterRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        String searchKeyword = request.getKeyword() == null ? "" : request.getKeyword();
        
        List<PorterProfile> porters = porterProfileRepository.findAllByVendorIdAndFilters(
                currentVendor.getVendorId(), 
                searchKeyword,
                request.getStatus());
                
        return porters.stream()
                .map(porterProfileMapper::toDto)
                .toList();
    }
}
