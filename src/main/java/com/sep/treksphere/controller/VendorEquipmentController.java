package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.UpdateVendorEquipmentRequest;
import com.sep.treksphere.dto.request.VendorEquipmentFilterRequest;
import com.sep.treksphere.dto.request.VendorEquipmentRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorEquipmentDto;
import com.sep.treksphere.service.VendorEquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/equipments")
@RequiredArgsConstructor
public class VendorEquipmentController {

    private final VendorEquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ApiResponse<VendorEquipmentDto> createEquipment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VendorEquipmentRequest request) {
        return ApiResponse.success(HttpStatus.CREATED, equipmentService.createEquipment(userDetails.getUsername(), request), MessageConstant.EQUIPMENT_CREATED_SUCCESSFULLY);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ApiResponse<VendorEquipmentDto> updateEquipment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateVendorEquipmentRequest request) {
        return ApiResponse.success(HttpStatus.OK, equipmentService.updateEquipment(userDetails.getUsername(), id, request), MessageConstant.EQUIPMENT_UPDATED_SUCCESSFULLY);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    public ApiResponse<Void> deleteEquipment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID id) {
        equipmentService.deleteEquipment(userDetails.getUsername(), id);
        return ApiResponse.success(HttpStatus.OK, null, MessageConstant.EQUIPMENT_DELETED_SUCCESSFULLY);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ApiResponse<VendorEquipmentDto> getEquipmentById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID id) {
        return ApiResponse.success(HttpStatus.OK, equipmentService.getEquipmentById(userDetails.getUsername(), id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ApiResponse<PaginationResponse<VendorEquipmentDto>> getEquipments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute VendorEquipmentFilterRequest request) {

        return ApiResponse.success(HttpStatus.OK, equipmentService.getEquipments(userDetails.getUsername(), request));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ApiResponse<List<VendorEquipmentDto>> getAllEquipmentsList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.success(HttpStatus.OK, equipmentService.getAllEquipmentsList(userDetails.getUsername(), keyword));
    }
}
