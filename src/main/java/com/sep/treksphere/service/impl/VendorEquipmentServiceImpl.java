package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.UpdateVendorEquipmentRequest;
import com.sep.treksphere.dto.request.VendorEquipmentFilterRequest;
import com.sep.treksphere.dto.request.VendorEquipmentRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorEquipmentDto;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorEquipment;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorEquipmentMapper;
import com.sep.treksphere.repository.VendorEquipmentRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.VendorEquipmentService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorEquipmentServiceImpl implements VendorEquipmentService {

    private final VendorEquipmentRepository equipmentRepository;
    private final VendorEquipmentMapper equipmentMapper;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;

    private Vendor getCurrentVendor(String email) {

        Optional<Vendor> managerVendor = vendorRepository.findByManager_Email(email);
        if (managerVendor.isPresent()) {
            return managerVendor.get();
        }

        Optional<VendorStaff> staff = vendorStaffRepository.findByUser_Email(email);
        if (staff.isPresent() && staff.get().getIsActive()) {
            return staff.get().getVendor();
        }

        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    @Override
    @Transactional
    public VendorEquipmentDto createEquipment(String email, VendorEquipmentRequest request) {
        Vendor currentVendor = getCurrentVendor(email);

        VendorEquipment equipment = equipmentMapper.toEntity(request);
        equipment.setVendor(currentVendor);
        
        VendorEquipment saved = equipmentRepository.save(equipment);
        return equipmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public VendorEquipmentDto updateEquipment(String email, UUID equipmentId, UpdateVendorEquipmentRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        
        VendorEquipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!equipment.getVendor().getVendorId().equals(currentVendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        equipmentMapper.updateEntityFromRequest(request, equipment);
        VendorEquipment saved = equipmentRepository.save(equipment);
        
        return equipmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteEquipment(String email, UUID equipmentId) {
        Vendor currentVendor = getCurrentVendor(email);
        
        VendorEquipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!equipment.getVendor().getVendorId().equals(currentVendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        equipment.setIsDeleted(true);
        equipmentRepository.save(equipment);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorEquipmentDto getEquipmentById(String email, UUID equipmentId) {
        Vendor currentVendor = getCurrentVendor(email);

        VendorEquipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (!equipment.getVendor().getVendorId().equals(currentVendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(equipment.getIsDeleted())) {
            throw new AppException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        return equipmentMapper.toDto(equipment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorEquipmentDto> getEquipments(String email, VendorEquipmentFilterRequest request) {
        Vendor currentVendor = getCurrentVendor(email);
        String searchKeyword = request.getKeyword() == null ? "" : request.getKeyword();
        Page<VendorEquipment> equipments = equipmentRepository
                .findByVendorIdAndKeyword(currentVendor.getVendorId(), searchKeyword, request.getPageable());
                
        return PaginationUtils.toPaginationResponse(equipments.map(equipmentMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorEquipmentDto> getAllEquipmentsList(String email, String keyword) {
        Vendor currentVendor = getCurrentVendor(email);
        String searchKeyword = keyword == null ? "" : keyword;
        List<VendorEquipment> equipments = equipmentRepository
                .findAllByVendorIdAndKeyword(currentVendor.getVendorId(), searchKeyword);

        return equipments.stream()
                .map(equipmentMapper::toDto)
                .collect(Collectors.toList());
    }
}
