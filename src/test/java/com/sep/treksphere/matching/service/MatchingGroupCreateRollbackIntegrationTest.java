package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.config.AuditConfig;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.MatchingGroupMapperImpl;
import com.sep.treksphere.matching.repository.CustomJourneyRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.user.AuthProvider;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditConfig.class, MatchingGroupService.class, MatchingGroupMapperImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MatchingGroupCreateRollbackIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private MatchingGroupService matchingGroupService;

    @Autowired
    private MatchingGroupRepository matchingGroupRepository;

    @Autowired
    private MatchingMemberRepository matchingMemberRepository;

    @Autowired
    private CustomJourneyRepository customJourneyRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private GroupTripRepository groupTripRepository;

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
    @DisplayName("[P2-S3] Persist đúng một Group, Custom Journey, Leader và GroupTrip")
    void createCustomJourneyGroup_PersistsExactlyOneCompleteAggregate() {
        long groupCountBefore = matchingGroupRepository.count();
        long memberCountBefore = matchingMemberRepository.count();
        long journeyCountBefore = customJourneyRepository.count();
        long tripCountBefore = groupTripRepository.count();
        User owner = createOwner();

        MatchingGroupDetailResponse response = matchingGroupService.createMatchingGroup(
                createRequest(),
                owner.getUserId()
        );

        assertThat(matchingGroupRepository.count()).isEqualTo(groupCountBefore + 1);
        assertThat(matchingMemberRepository.count()).isEqualTo(memberCountBefore + 1);
        assertThat(customJourneyRepository.count()).isEqualTo(journeyCountBefore + 1);
        assertThat(groupTripRepository.count()).isEqualTo(tripCountBefore + 1);
        assertThat(response.getSourceType()).isEqualTo(MatchingGroupSourceType.CUSTOM_JOURNEY);
        assertThat(response.getMembers()).singleElement().satisfies(member -> {
            assertThat(member.getRole()).isEqualTo(MatchingRole.LEADER);
            assertThat(member.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
        });
        assertThat(groupTripRepository.findAll()).anySatisfy(trip -> {
            assertThat(trip.getMatchingGroup().getMatchingGroupId()).isEqualTo(response.getMatchingGroupId());
            assertThat(trip.getStatus()).isEqualTo(GroupTripStatus.PLANNED);
        });

        // Test discovery query:
        MatchingGroupFilterRequest filter = new MatchingGroupFilterRequest();
        PaginationResponse<MatchingGroupResponse> searchResult = matchingGroupService.getMatchingGroups(filter);
        assertThat(searchResult.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("[P2-S3] Lỗi khi lưu GroupTrip rollback Group, Custom Journey và Leader")
    void createCustomJourneyGroup_WhenTripSaveFails_RollsBackWholeAggregate() {
        long groupCountBefore = matchingGroupRepository.count();
        long memberCountBefore = matchingMemberRepository.count();
        long journeyCountBefore = customJourneyRepository.count();
        long userCountBefore = userRepository.count();
        User owner = createOwner();
        MatchingGroupCreateRequest request = createRequest();
        doThrow(new IllegalStateException("simulated trip persistence failure"))
                .when(groupTripRepository).save(any(GroupTrip.class));

        assertThatThrownBy(() -> matchingGroupService.createMatchingGroup(request, owner.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated trip persistence failure");

        assertThat(matchingGroupRepository.count()).isEqualTo(groupCountBefore);
        assertThat(matchingMemberRepository.count()).isEqualTo(memberCountBefore);
        assertThat(customJourneyRepository.count()).isEqualTo(journeyCountBefore);
        assertThat(userRepository.count()).isEqualTo(userCountBefore + 1);
    }

    private User createOwner() {
        User owner = new User();
        owner.setEmail("p2-s3-owner-" + UUID.randomUUID() + "@example.com");
        owner.setFullName("P2-S3 Integration Owner");
        owner.setStatus(UserStatus.ACTIVE);
        owner.setEmailVerified(true);
        owner.setProvider(AuthProvider.LOCAL);
        return userRepository.saveAndFlush(owner);
    }

    private MatchingGroupCreateRequest createRequest() {
        LocalDate targetDate = LocalDate.now().plusDays(10);

        CustomJourneyCreateRequest journey = new CustomJourneyCreateRequest();
        journey.setTitle("Rollback Journey");
        journey.setDifficulty(JourneyDifficulty.HARD);
        journey.setStartDate(targetDate);
        journey.setEndDate(targetDate.plusDays(2));

        MatchingGroupCreateRequest request = new MatchingGroupCreateRequest();
        request.setSourceType(MatchingGroupSourceType.CUSTOM_JOURNEY);
        request.setCustomJourney(journey);
        request.setGroupName("Rollback Group");
        request.setMaxSize(5);
        request.setTargetDate(targetDate);
        request.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        request.setScheduledStartAt(targetDate.atTime(7, 0));
        return request;
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start embedded PostgreSQL", exception);
        }
    }
}
