package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.PorterProfileFilterRequest;
import com.sep.treksphere.dto.request.PorterProfileRequest;
import com.sep.treksphere.dto.request.UpdatePorterProfileRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.PorterProfileDto;
import com.sep.treksphere.service.PorterProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/porters")
@RequiredArgsConstructor
@Tag(name = "Vendor Porter Profiles", description = "Quản lý hồ sơ Porter của Vendor")
public class PorterProfileController {

    private final PorterProfileService porterProfileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Tạo mới hồ sơ porter")
    public ApiResponse<PorterProfileDto> createPorter(
            Principal principal,
            @Valid @ModelAttribute PorterProfileRequest request) {
        return ApiResponse.success(
                HttpStatus.CREATED,
                porterProfileService.createPorter(principal.getName(), request),
                MessageConstant.PORTER_CREATED_SUCCESSFULLY
        );
    }

    @PutMapping(value = "/{porterId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Cập nhật hồ sơ porter")
    public ApiResponse<PorterProfileDto> updatePorter(
            Principal principal,
            @PathVariable UUID porterId,
            @Valid @ModelAttribute UpdatePorterProfileRequest request) {
        return ApiResponse.success(
                HttpStatus.OK,
                porterProfileService.updatePorter(principal.getName(), porterId, request),
                MessageConstant.PORTER_UPDATED_SUCCESSFULLY
        );
    }

    @DeleteMapping("/{porterId}")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @Operation(summary = "Xóa hồ sơ porter (Xóa mềm)")
    public ApiResponse<Void> deletePorter(
            Principal principal,
            @PathVariable UUID porterId) {
        porterProfileService.deletePorter(principal.getName(), porterId);
        return ApiResponse.success(
                HttpStatus.OK,
                null,
                MessageConstant.PORTER_DELETED_SUCCESSFULLY
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Lấy danh sách hồ sơ porter phân trang")
    public ApiResponse<PaginationResponse<PorterProfileDto>> getPorters(
            Principal principal,
            @Valid @ModelAttribute PorterProfileFilterRequest request) {
        return ApiResponse.success(HttpStatus.OK, porterProfileService.getPorters(principal.getName(), request), MessageConstant.PORTER_LIST_FETCHED_SUCCESSFULLY);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Lấy toàn bộ danh sách hồ sơ porter")
    public ApiResponse<List<PorterProfileDto>> getAllPorters(
            Principal principal,
            @Valid @ModelAttribute PorterProfileFilterRequest request) {
        return ApiResponse.success(HttpStatus.OK, porterProfileService.getAllPortersList(principal.getName(), request), MessageConstant.PORTER_LIST_FETCHED_SUCCESSFULLY);
    }
}
