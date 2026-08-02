package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import com.sep.treksphere.mapper.VendorApplicationMapper;
import com.sep.treksphere.mapper.VendorMapper;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorApplicationServiceImplTest {

    @Mock
    private VendorApplicationRepository vendorApplicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorApplicationMapper vendorApplicationMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private VendorMapper vendorMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private VendorApplicationServiceImpl service;

    @Test
    void getApplications_OnlyQueriesAdminVisibleStatuses() {
        VendorApplicationFilterRequest request = new VendorApplicationFilterRequest();
        Page<VendorApplication> emptyPage = new PageImpl<>(List.of(), request.getPageable(), 0);
        when(vendorApplicationRepository.findAllApplicationsWithFilter(
                any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        var response = service.getApplications(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<ApplicationStatus>> statusesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(vendorApplicationRepository).findAllApplicationsWithFilter(
                statusesCaptor.capture(), isNull(), isNull(), any(Pageable.class));

        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(
                ApplicationStatus.PENDING,
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED
        );
        assertThat(statusesCaptor.getValue()).doesNotContain(ApplicationStatus.DRAFT);
        assertThat(response.getContent()).isEmpty();
    }
}
