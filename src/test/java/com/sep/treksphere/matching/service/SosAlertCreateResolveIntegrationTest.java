package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.config.AuditConfig;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.matching.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.matching.dto.response.SosAlertResponse;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.SosAlert;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.IncidentType;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.SosAlertStatus;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.SosAlertRepository;
import com.sep.treksphere.matching.service.impl.SosAlertServiceImpl;
import com.sep.treksphere.notification.NotificationService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditConfig.class, SosAlertServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SosAlertCreateResolveIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private SosAlertService sosAlertService;

    @Autowired
    private SosAlertRepository sosAlertRepository;

    @Autowired
    private MatchingGroupRepository matchingGroupRepository;

    @Autowired
    private MatchingMemberRepository matchingMemberRepository;

    @Autowired
    private GroupTripRepository groupTripRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

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
    @DisplayName("createAlert với latitude/longitude = null vẫn tạo thành công (GPS optional)")
    void createAlert_WithoutGps_PersistsSuccessfully() {
        User leader = createUser("leader-null-gps");
        User sender = createUser("sender-null-gps");
        MatchingGroup group = createGroup(leader);
        createMember(group, leader, MatchingRole.LEADER);
        createMember(group, sender, MatchingRole.MEMBER);
        createTrip(group, GroupTripStatus.IN_PROGRESS);

        CreateSosAlertRequest request = CreateSosAlertRequest.builder()
                .incidentTypeCode(IncidentType.LOST)
                .message("Lạc đường, không có GPS")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        SosAlertResponse response = sosAlertService.createAlert(group.getMatchingGroupId(), request, sender.getUserId());

        assertThat(response.getStatus()).isEqualTo(SosAlertStatus.OPEN);
        assertThat(response.getLatitude()).isNull();
        assertThat(response.getLongitude()).isNull();

        SosAlert persisted = sosAlertRepository.findById(response.getSosAlertId()).orElseThrow();
        assertThat(persisted.getLatitude()).isNull();
        assertThat(persisted.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Concurrent resolve: 2 thread cùng gọi resolve trên cùng alert -> chỉ đúng 1 thành công")
    void resolve_ConcurrentCalls_OnlyOneSucceeds() throws InterruptedException {
        User leader = createUser("leader-concurrent");
        User sender = createUser("sender-concurrent");
        MatchingGroup group = createGroup(leader);
        createMember(group, leader, MatchingRole.LEADER);
        createMember(group, sender, MatchingRole.MEMBER);
        GroupTrip trip = createTrip(group, GroupTripStatus.IN_PROGRESS);

        SosAlert alert = new SosAlert();
        alert.setGroupTrip(trip);
        alert.setSender(sender);
        alert.setIncidentTypeCode(IncidentType.WEATHER);
        alert.setStatus(SosAlertStatus.OPEN);
        alert.setIdempotencyKey(UUID.randomUUID().toString());
        SosAlert savedAlert = sosAlertRepository.saveAndFlush(alert);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        Runnable resolveTask = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                sosAlertService.resolve(group.getMatchingGroupId(), savedAlert.getSosAlertId(), leader.getUserId());
                successCount.incrementAndGet();
            } catch (AppException | InterruptedException ex) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(resolveTask);
        executor.submit(resolveTask);
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        SosAlert finalAlert = sosAlertRepository.findById(savedAlert.getSosAlertId()).orElseThrow();
        assertThat(finalAlert.getStatus()).isEqualTo(SosAlertStatus.RESOLVED);
        assertThat(finalAlert.getResolvedBy().getUserId()).isEqualTo(leader.getUserId());
    }

    private User createUser(String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setFullName("SOS Integration " + label);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProvider(AuthProvider.LOCAL);
        return userRepository.saveAndFlush(user);
    }

    private MatchingGroup createGroup(User owner) {
        MatchingGroup group = new MatchingGroup();
        group.setOwner(owner);
        group.setGroupName("SOS Integration Group " + UUID.randomUUID());
        group.setMaxSize(5);
        group.setCurrentSize(2);
        group.setTargetDate(LocalDate.now().plusDays(3));
        group.setMatchingDeadline(LocalDateTime.now().minusDays(1));
        group.setStatus(MatchingGroupStatus.IN_PROGRESS);
        return matchingGroupRepository.saveAndFlush(group);
    }

    private void createMember(MatchingGroup group, User user, MatchingRole role) {
        MatchingMember member = new MatchingMember();
        member.setMatchingGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(JoinStatus.ACCEPTED);
        matchingMemberRepository.saveAndFlush(member);
    }

    private GroupTrip createTrip(MatchingGroup group, GroupTripStatus status) {
        GroupTrip trip = new GroupTrip();
        trip.setMatchingGroup(group);
        trip.setStatus(status);
        trip.setScheduledStartAt(LocalDateTime.now().minusHours(1));
        if (status == GroupTripStatus.IN_PROGRESS) {
            trip.setStartedAt(LocalDateTime.now().minusMinutes(30));
        }
        return groupTripRepository.saveAndFlush(trip);
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start embedded PostgreSQL", exception);
        }
    }
}
