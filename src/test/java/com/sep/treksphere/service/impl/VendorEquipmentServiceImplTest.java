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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorEquipmentServiceImplTest {

    @Mock
    private VendorEquipmentRepository equipmentRepository;
    @Mock
    private VendorEquipmentMapper equipmentMapper;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorStaffRepository vendorStaffRepository;

    @InjectMocks
    private VendorEquipmentServiceImpl equipmentService;

    private Vendor mockVendor;
    private VendorEquipment mockEquipment;
    private final String EMAIL_MANAGER = "manager@vendor.com";
    private final String EMAIL_STAFF = "staff@vendor.com";
    private final String EMAIL_NOT_FOUND = "notfound@vendor.com";
    private final UUID VENDOR_ID = UUID.randomUUID();
    private final UUID EQUIPMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockVendor = new Vendor();
        mockVendor.setVendorId(VENDOR_ID);

        mockEquipment = new VendorEquipment();
        mockEquipment.setEquipmentId(EQUIPMENT_ID);
        mockEquipment.setVendor(mockVendor);
        mockEquipment.setIsDeleted(false);
    }

    private void setupManagerAuth() {
        when(vendorRepository.findByManager_Email(EMAIL_MANAGER)).thenReturn(Optional.of(mockVendor));
    }

    private void setupStaffAuth(boolean isActive) {
        VendorStaff staff = new VendorStaff();
        staff.setVendor(mockVendor);
        staff.setIsActive(isActive);
        when(vendorRepository.findByManager_Email(EMAIL_STAFF)).thenReturn(Optional.empty());
        when(vendorStaffRepository.findByUser_Email(EMAIL_STAFF)).thenReturn(Optional.of(staff));
    }

    // ==========================================
    // Test getCurrentVendor (via createEquipment)
    // ==========================================
    @Test
    void testAuth_Manager_Success() {
        setupManagerAuth();
        VendorEquipmentRequest req = new VendorEquipmentRequest();
        when(equipmentMapper.toEntity(any())).thenReturn(new VendorEquipment());
        when(equipmentRepository.save(any())).thenReturn(mockEquipment);
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        assertDoesNotThrow(() -> equipmentService.createEquipment(EMAIL_MANAGER, req));
    }

    @Test
    void testAuth_StaffActive_Success() {
        setupStaffAuth(true);
        VendorEquipmentRequest req = new VendorEquipmentRequest();
        when(equipmentMapper.toEntity(any())).thenReturn(new VendorEquipment());
        when(equipmentRepository.save(any())).thenReturn(mockEquipment);
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        assertDoesNotThrow(() -> equipmentService.createEquipment(EMAIL_STAFF, req));
    }

    @Test
    void testAuth_StaffInactive_ThrowAccessDenied() {
        setupStaffAuth(false);
        VendorEquipmentRequest req = new VendorEquipmentRequest();

        AppException ex = assertThrows(AppException.class, () -> equipmentService.createEquipment(EMAIL_STAFF, req));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void testAuth_NotFound_ThrowAccessDenied() {
        when(vendorRepository.findByManager_Email(EMAIL_NOT_FOUND)).thenReturn(Optional.empty());
        when(vendorStaffRepository.findByUser_Email(EMAIL_NOT_FOUND)).thenReturn(Optional.empty());
        VendorEquipmentRequest req = new VendorEquipmentRequest();

        AppException ex = assertThrows(AppException.class, () -> equipmentService.createEquipment(EMAIL_NOT_FOUND, req));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // ==========================================
    // Test updateEquipment
    // ==========================================
    @Test
    void testUpdateEquipment_Success() {
        setupManagerAuth();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(mockEquipment));
        when(equipmentRepository.save(any())).thenReturn(mockEquipment);
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        UpdateVendorEquipmentRequest req = new UpdateVendorEquipmentRequest();
        assertDoesNotThrow(() -> equipmentService.updateEquipment(EMAIL_MANAGER, EQUIPMENT_ID, req));
        verify(equipmentMapper).updateEntityFromRequest(req, mockEquipment);
    }

    @Test
    void testUpdateEquipment_NotFound_ThrowsException() {
        setupManagerAuth();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

        UpdateVendorEquipmentRequest req = new UpdateVendorEquipmentRequest();
        AppException ex = assertThrows(AppException.class, () -> equipmentService.updateEquipment(EMAIL_MANAGER, EQUIPMENT_ID, req));
        assertEquals(ErrorCode.EQUIPMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testUpdateEquipment_DifferentVendor_ThrowsAccessDenied() {
        setupManagerAuth();
        
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(UUID.randomUUID()); // Different vendor
        VendorEquipment otherEquipment = new VendorEquipment();
        otherEquipment.setVendor(otherVendor);

        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(otherEquipment));

        UpdateVendorEquipmentRequest req = new UpdateVendorEquipmentRequest();
        AppException ex = assertThrows(AppException.class, () -> equipmentService.updateEquipment(EMAIL_MANAGER, EQUIPMENT_ID, req));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // ==========================================
    // Test deleteEquipment
    // ==========================================
    @Test
    void testDeleteEquipment_Success() {
        setupManagerAuth();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(mockEquipment));
        
        equipmentService.deleteEquipment(EMAIL_MANAGER, EQUIPMENT_ID);
        
        assertTrue(mockEquipment.getIsDeleted());
        verify(equipmentRepository).save(mockEquipment);
    }

    // ==========================================
    // Test getEquipmentById
    // ==========================================
    @Test
    void testGetEquipmentById_Success() {
        setupManagerAuth();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(mockEquipment));
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        assertDoesNotThrow(() -> equipmentService.getEquipmentById(EMAIL_MANAGER, EQUIPMENT_ID));
    }

    @Test
    void testGetEquipmentById_IsDeleted_ThrowsNotFound() {
        setupManagerAuth();
        mockEquipment.setIsDeleted(true);
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(mockEquipment));

        AppException ex = assertThrows(AppException.class, () -> equipmentService.getEquipmentById(EMAIL_MANAGER, EQUIPMENT_ID));
        assertEquals(ErrorCode.EQUIPMENT_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // Test getEquipments
    // ==========================================
    @Test
    void testGetEquipments_Success() {
        setupManagerAuth();
        VendorEquipmentFilterRequest req = new VendorEquipmentFilterRequest();
        req.setPage(0);
        req.setSize(10);
        req.setKeyword("test");

        Page<VendorEquipment> page = new PageImpl<>(Collections.singletonList(mockEquipment));
        
        when(equipmentRepository.findByVendorIdAndKeyword(eq(VENDOR_ID), eq("test"), any(Pageable.class))).thenReturn(page);
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        PaginationResponse<VendorEquipmentDto> result = equipmentService.getEquipments(EMAIL_MANAGER, req);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllEquipmentsList_Success() {
        setupManagerAuth();
        String keyword = "test";
        List<VendorEquipment> list = Collections.singletonList(mockEquipment);

        when(equipmentRepository.findAllByVendorIdAndKeyword(VENDOR_ID, keyword)).thenReturn(list);
        when(equipmentMapper.toDto(any())).thenReturn(new VendorEquipmentDto());

        List<VendorEquipmentDto> result = equipmentService.getAllEquipmentsList(EMAIL_MANAGER, keyword);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
