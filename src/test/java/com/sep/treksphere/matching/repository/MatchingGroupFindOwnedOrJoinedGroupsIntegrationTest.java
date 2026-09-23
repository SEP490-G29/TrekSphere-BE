package com.sep.treksphere.matching.repository;

import com.sep.treksphere.common.config.AuditConfig;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.user.enums.AuthProvider;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.repository.UserRepository;
import com.sep.treksphere.user.enums.UserStatus;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug đã xác nhận: sau khi bầu Trưởng nhóm mới (role đổi trong MatchingMember, KHÔNG đổi
 * matchingGroup.owner_id), leader mới không thấy nhóm trong "Quản lý nhóm của tôi" vì query cũ
 * hard-code role=MEMBER ở nhánh "joined" và lọc tab Leader theo owner thay vì role thật.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditConfig.class)
class MatchingGroupFindOwnedOrJoinedGroupsIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private MatchingGroupRepository matchingGroupRepository;

    @Autowired
    private MatchingMemberRepository matchingMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @Test
    @DisplayName("Sau khi role đổi (B thành LEADER, A/creator thành MEMBER): cả 2 vẫn thấy nhóm trong list mặc định, " +
            "tab Leader chỉ trả B, tab Member chỉ trả A")
    void findOwnedOrJoinedGroups_AfterLeaderRoleSwap_ReflectsActualRoleNotOwner() {
        User creator = createUser("creator");
        User newLeader = createUser("new-leader");

        MatchingGroup group = new MatchingGroup();
        group.setOwner(creator);
        group.setGroupName("Test Group " + UUID.randomUUID());
        group.setMaxSize(5);
        group.setCurrentSize(2);
        group.setTargetDate(LocalDate.now().plusDays(3));
        group.setMatchingDeadline(LocalDateTime.now().minusDays(1));
        group.setStatus(MatchingGroupStatus.IN_PROGRESS);
        MatchingGroup savedGroup = matchingGroupRepository.saveAndFlush(group);

        // Trạng thái SAU bầu cử: creator (owner) đã bị demote xuống MEMBER, newLeader lên LEADER.
        MatchingMember creatorMember = new MatchingMember();
        creatorMember.setMatchingGroup(savedGroup);
        creatorMember.setUser(creator);
        creatorMember.setRole(MatchingRole.MEMBER);
        creatorMember.setStatus(JoinStatus.ACCEPTED);
        matchingMemberRepository.saveAndFlush(creatorMember);

        MatchingMember newLeaderMember = new MatchingMember();
        newLeaderMember.setMatchingGroup(savedGroup);
        newLeaderMember.setUser(newLeader);
        newLeaderMember.setRole(MatchingRole.LEADER);
        newLeaderMember.setStatus(JoinStatus.ACCEPTED);
        matchingMemberRepository.saveAndFlush(newLeaderMember);

        // 1) Không lọc role: CẢ creator lẫn newLeader đều phải thấy nhóm này trong "của tôi".
        Page<MatchingGroup> creatorAll = matchingGroupRepository.findOwnedOrJoinedGroups(
                creator.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, null, null, null, null, "", PageRequest.of(0, 20));
        Page<MatchingGroup> newLeaderAll = matchingGroupRepository.findOwnedOrJoinedGroups(
                newLeader.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, null, null, null, null, "", PageRequest.of(0, 20));

        assertThat(creatorAll.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .contains(savedGroup.getMatchingGroupId());
        assertThat(newLeaderAll.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .contains(savedGroup.getMatchingGroupId());

        // 2) Lọc role=LEADER: chỉ newLeader thấy, creator (giờ chỉ là MEMBER) không còn thấy nữa.
        Page<MatchingGroup> creatorLeaderTab = matchingGroupRepository.findOwnedOrJoinedGroups(
                creator.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, MatchingRole.LEADER, null, null, null, "",
                PageRequest.of(0, 20));
        Page<MatchingGroup> newLeaderLeaderTab = matchingGroupRepository.findOwnedOrJoinedGroups(
                newLeader.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, MatchingRole.LEADER, null, null, null, "",
                PageRequest.of(0, 20));

        assertThat(creatorLeaderTab.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .doesNotContain(savedGroup.getMatchingGroupId());
        assertThat(newLeaderLeaderTab.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .contains(savedGroup.getMatchingGroupId());

        // 3) Lọc role=MEMBER: chỉ creator (giờ là MEMBER) thấy, newLeader (giờ là LEADER) không thấy.
        Page<MatchingGroup> creatorMemberTab = matchingGroupRepository.findOwnedOrJoinedGroups(
                creator.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, MatchingRole.MEMBER, null, null, null, "",
                PageRequest.of(0, 20));
        Page<MatchingGroup> newLeaderMemberTab = matchingGroupRepository.findOwnedOrJoinedGroups(
                newLeader.getUserId(), MatchingRole.LEADER, MatchingRole.MEMBER, JoinStatus.ACCEPTED, MatchingRole.MEMBER, null, null, null, "",
                PageRequest.of(0, 20));

        assertThat(creatorMemberTab.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .contains(savedGroup.getMatchingGroupId());
        assertThat(newLeaderMemberTab.getContent()).extracting(MatchingGroup::getMatchingGroupId)
                .doesNotContain(savedGroup.getMatchingGroupId());
    }

    private User createUser(String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setFullName("Owner/Leader Test " + label);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProvider(AuthProvider.LOCAL);
        return userRepository.saveAndFlush(user);
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start embedded PostgreSQL", exception);
        }
    }
}
