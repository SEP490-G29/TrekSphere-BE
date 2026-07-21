package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.UpdateVendorEquipmentRequest;
import com.sep.treksphere.dto.request.VendorEquipmentFilterRequest;
import com.sep.treksphere.dto.request.VendorEquipmentRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorEquipmentDto;

import java.util.List;
import java.util.UUID;

public interface VendorEquipmentService {
    VendorEquipmentDto createEquipment(String email, VendorEquipmentRequest request);
    VendorEquipmentDto updateEquipment(String email, UUID equipmentId, UpdateVendorEquipmentRequest request);
    void deleteEquipment(String email, UUID equipmentId);
    VendorEquipmentDto getEquipmentById(String email, UUID equipmentId);
    PaginationResponse<VendorEquipmentDto> getEquipments(String email, VendorEquipmentFilterRequest request);
    List<VendorEquipmentDto> getAllEquipmentsList(String email, String keyword);
}
