package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.MatchingGroupMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.MatchingGroupService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final TourRepository tourRepository;
    private final MatchingGroupMapper matchingGroupMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter) {
        log.info("Fetching available matching groups with filters: tourId={}, targetDate={}",
                filter.getTourId(), filter.getTargetDate());

        // Tìm kiếm các nhóm ghép bạn đồng hành đang mở (OPEN) và chưa bị xóa
        Page<MatchingGroup> groups = matchingGroupRepository.findAvailableMatchingGroups(
                MatchingGroupStatus.OPEN,
                filter.getTourId(),
                filter.getTargetDate(),
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(matchingGroupMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchingGroupDetailResponse getMatchingGroupById(UUID id) {
        log.info("Fetching matching group detail: id={}", id);

        // Tìm kiếm nhóm ghép theo ID (JOIN FETCH tour, owner, members)
        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        // Ánh xạ thông tin cơ bản của nhóm ghép sang DTO (không bao gồm members)
        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(matchingGroup);

        // Lọc danh sách thành viên: Chỉ giữ lại các thành viên đã được ACCEPTED và chưa bị xóa mềm
        List<MatchingMemberResponse> acceptedMembers = matchingGroup.getMembers().stream()
                .filter(member -> member.getStatus() == JoinStatus.ACCEPTED
                        && !Boolean.TRUE.equals(member.getIsDeleted()))
                .map(matchingGroupMapper::toMemberResponse)
                .toList();

        response.setMembers(acceptedMembers);

        return response;
    }

    @Override
    @Transactional
    public MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Creating matching group: ownerId={}, groupName={}", currentUser.getUserId(), request.getGroupName());

        // 1. Kiểm tra chống spam: xem user có đang là OWNER của nhóm ghép nào status OPEN hay chưa
        boolean hasActiveGroup = matchingGroupRepository.existsByOwnerAndStatusAndIsDeletedFalse(currentUser, MatchingGroupStatus.OPEN);
        if (hasActiveGroup) {
            throw new AppException(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);
        }

        // 2. Tìm Tour
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        // 3. Kiểm tra ngày khởi hành dự kiến
        if (request.getTargetDate().isBefore(LocalDate.now()) || request.getTargetDate().isEqual(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_TARGET_DATE);
        }

        // 4. Kiểm tra deadline
        if (request.getMatchingDeadline().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (request.getMatchingDeadline().toLocalDate().isAfter(request.getTargetDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        // 5. Khởi tạo MatchingGroup
        MatchingGroup matchingGroup = new MatchingGroup();
        matchingGroup.setTour(tour);
        matchingGroup.setOwner(currentUser);
        matchingGroup.setGroupName(request.getGroupName());
        matchingGroup.setDescription(request.getDescription());
        matchingGroup.setMaxSize(request.getMaxSize());
        matchingGroup.setCurrentSize(1);
        matchingGroup.setTargetDate(request.getTargetDate());
        matchingGroup.setMatchingDeadline(request.getMatchingDeadline());
        matchingGroup.setStatus(MatchingGroupStatus.OPEN);

        // 6. Tạo thành viên trưởng nhóm (OWNER)
        MatchingMember ownerMember = new MatchingMember();
        ownerMember.setMatchingGroup(matchingGroup);
        ownerMember.setUser(currentUser);
        ownerMember.setRole(MatchingRole.OWNER);
        ownerMember.setStatus(JoinStatus.ACCEPTED);

        // Thêm vào list members của matchingGroup để JPA tự động cascade save
        matchingGroup.getMembers().add(ownerMember);

        // 7. Lưu vào DB
        MatchingGroup savedGroup = matchingGroupRepository.save(matchingGroup);

        // 8. Map kết quả trả về
        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(savedGroup);

        // Gán members của response (chỉ chứa chính owner vừa được tạo)
        List<MatchingMemberResponse> memberResponses = savedGroup.getMembers().stream()
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(matchingGroupMapper::toMemberResponse)
                .toList();
        response.setMembers(memberResponses);

        return response;
    }
}
