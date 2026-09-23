package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.config.AuditConfig;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.UpdateCheckpointProgressRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.CheckpointProgressAction;
import com.sep.treksphere.matching.enums.CheckpointProgressStatus;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.CustomJourneyMapperImpl;
import com.sep.treksphere.matching.repository.CustomJourneyCheckpointRepository;
import com.sep.treksphere.matching.repository.CustomJourneyRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.CustomJourneyServiceImpl;
import com.sep.treksphere.notification.service.NotificationService;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditConfig.class, CustomJourneyServiceImpl.class, CustomJourneyMapperImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomJourneyCheckpointCheckInConcurrencyIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private CustomJourneyService customJourneyService;

    @Autowired
    private CustomJourneyRepository customJourneyRepository;

    @Autowired
    private CustomJourneyCheckpointRepository checkpointRepository;

    @Autowired
    private MatchingGroupRepository matchingGroupRepository;

    @Autowired
    private GroupTripRepository groupTripRepository;

    @Autowired
    private MatchingMemberRepository matchingMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

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
    @DisplayName("Concurrent check-in cùng 1 checkpoint: chỉ 1 request thành công nhờ pessimistic lock, không mất dữ liệu")
    void updateCheckpointProgress_ConcurrentRequests_OnlyOneSucceeds() throws InterruptedException {
        User leader = createUser("leader-checkin-concurrent");
        MatchingGroup group = createGroup(leader);
        createMember(group, leader, MatchingRole.LEADER);

        GroupTrip trip = new GroupTrip();
        trip.setMatchingGroup(group);
        trip.setStatus(GroupTripStatus.IN_PROGRESS);
        trip.setScheduledStartAt(LocalDateTime.now().minusHours(1));
        groupTripRepository.saveAndFlush(trip);

        CustomJourney journey = new CustomJourney();
        journey.setMatchingGroup(group);
        journey.setTitle("Concurrency Checkin Journey");
        journey.setDifficulty(JourneyDifficulty.MODERATE);
        journey.setStartDate(LocalDate.now().plusDays(1));
        journey.setEndDate(LocalDate.now().plusDays(2));
        journey.setIsLocked(false);
        CustomJourney savedJourney = customJourneyRepository.saveAndFlush(journey);

        CustomJourneyCheckpoint checkpoint = new CustomJourneyCheckpoint();
        checkpoint.setCustomJourney(savedJourney);
        checkpoint.setDayNo(1);
        checkpoint.setCheckpointOrder(1);
        checkpoint.setTitle("Base Camp");
        CustomJourneyCheckpoint savedCheckpoint = checkpointRepository.saveAndFlush(checkpoint);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger alreadySetCount = new AtomicInteger();
        UpdateCheckpointProgressRequest request = UpdateCheckpointProgressRequest.builder()
                .status(CheckpointProgressAction.CHECKED_IN)
                .build();

        Runnable checkInTask = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                customJourneyService.updateCheckpointProgress(
                        group.getMatchingGroupId(), savedCheckpoint.getCustomJourneyCheckpointId(), request, leader.getUserId());
                successCount.incrementAndGet();
            } catch (AppException ex) {
                if (ex.getErrorCode() == ErrorCode.CUSTOM_JOURNEY_CHECKPOINT_PROGRESS_ALREADY_SET) {
                    alreadySetCount.incrementAndGet();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(checkInTask);
        executor.submit(checkInTask);
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(alreadySetCount.get()).isEqualTo(1);

        CustomJourneyCheckpoint finalCheckpoint = checkpointRepository.findById(savedCheckpoint.getCustomJourneyCheckpointId())
                .orElseThrow();
        assertThat(finalCheckpoint.getStatus()).isEqualTo(CheckpointProgressStatus.CHECKED_IN);
        assertThat(finalCheckpoint.getProgressUpdatedAt()).isNotNull();
        assertThat(finalCheckpoint.getProgressUpdatedBy()).isNotNull();
    }

    @Test
    @DisplayName("updateCheckpointProgress - ném lỗi trên DB thật khi chuyến đi chưa IN_PROGRESS")
    void updateCheckpointProgress_TripNotInProgress_OnRealDatabase() {
        User leader = createUser("leader-checkin-not-active");
        MatchingGroup group = createGroup(leader);
        createMember(group, leader, MatchingRole.LEADER);

        GroupTrip trip = new GroupTrip();
        trip.setMatchingGroup(group);
        trip.setStatus(GroupTripStatus.PLANNED);
        trip.setScheduledStartAt(LocalDateTime.now().plusDays(3));
        groupTripRepository.saveAndFlush(trip);

        CustomJourney journey = new CustomJourney();
        journey.setMatchingGroup(group);
        journey.setTitle("Not Active Journey");
        journey.setDifficulty(JourneyDifficulty.EASY);
        journey.setStartDate(LocalDate.now().plusDays(1));
        journey.setEndDate(LocalDate.now().plusDays(2));
        journey.setIsLocked(false);
        CustomJourney savedJourney = customJourneyRepository.saveAndFlush(journey);

        CustomJourneyCheckpoint checkpoint = new CustomJourneyCheckpoint();
        checkpoint.setCustomJourney(savedJourney);
        checkpoint.setDayNo(1);
        checkpoint.setCheckpointOrder(1);
        checkpoint.setTitle("Base Camp");
        CustomJourneyCheckpoint savedCheckpoint = checkpointRepository.saveAndFlush(checkpoint);

        UUID groupId = group.getMatchingGroupId();
        UUID checkpointId = savedCheckpoint.getCustomJourneyCheckpointId();
        UUID leaderId = leader.getUserId();
        UpdateCheckpointProgressRequest request = UpdateCheckpointProgressRequest.builder()
                .status(CheckpointProgressAction.CHECKED_IN)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> customJourneyService.updateCheckpointProgress(groupId, checkpointId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_JOURNEY_PROGRESS_TRIP_NOT_ACTIVE);

        CustomJourneyCheckpointResponse checkpoints = customJourneyService
                .getCheckpoints(groupId, leaderId).stream().findFirst().orElseThrow();
        assertThat(checkpoints.getStatus()).isEqualTo(CheckpointProgressStatus.PENDING);
    }

    private User createUser(String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setFullName("Checkin Integration " + label);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProvider(AuthProvider.LOCAL);
        return userRepository.saveAndFlush(user);
    }

    private MatchingGroup createGroup(User owner) {
        MatchingGroup group = new MatchingGroup();
        group.setOwner(owner);
        group.setGroupName("Checkin Integration Group " + UUID.randomUUID());
        group.setMaxSize(5);
        group.setCurrentSize(1);
        group.setTargetDate(LocalDate.now().plusDays(1));
        group.setMatchingDeadline(LocalDateTime.now().minusDays(1));
        group.setStatus(MatchingGroupStatus.IN_PROGRESS);
        return matchingGroupRepository.saveAndFlush(group);
    }

    private MatchingMember createMember(MatchingGroup group, User user, MatchingRole role) {
        MatchingMember member = new MatchingMember();
        member.setMatchingGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(JoinStatus.ACCEPTED);
        return matchingMemberRepository.saveAndFlush(member);
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start embedded PostgreSQL", exception);
        }
    }
}
