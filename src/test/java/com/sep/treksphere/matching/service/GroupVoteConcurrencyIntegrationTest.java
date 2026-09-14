package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.config.AuditConfig;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.entity.GroupVote;
import com.sep.treksphere.matching.entity.GroupVoteOption;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import com.sep.treksphere.matching.repository.GroupVoteBallotRepository;
import com.sep.treksphere.matching.repository.GroupVoteOptionRepository;
import com.sep.treksphere.matching.repository.GroupVoteRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupVoteServiceImpl;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditConfig.class, GroupVoteServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GroupVoteConcurrencyIntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private GroupVoteService groupVoteService;

    @Autowired
    private GroupVoteRepository groupVoteRepository;

    @Autowired
    private GroupVoteOptionRepository groupVoteOptionRepository;

    @Autowired
    private GroupVoteBallotRepository groupVoteBallotRepository;

    @Autowired
    private MatchingGroupRepository matchingGroupRepository;

    @Autowired
    private MatchingMemberRepository matchingMemberRepository;

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
    @DisplayName("Concurrent last ballots: 2 thread cùng cast phiếu cuối -> vote chỉ tự đóng đúng 1 lần, không mất phiếu")
    void castBallot_ConcurrentLastBallots_ClosesExactlyOnceWithCorrectTally() throws InterruptedException {
        User leader = createUser("leader-vote-concurrent");
        User member = createUser("member-vote-concurrent");
        MatchingGroup group = createGroup(leader);
        MatchingMember leaderMember = createMember(group, leader, MatchingRole.LEADER);
        MatchingMember memberEntity = createMember(group, member, MatchingRole.MEMBER);

        GroupVote vote = new GroupVote();
        vote.setMatchingGroup(group);
        vote.setVoteType(VoteType.OTHER);
        vote.setTitle("Chọn quán ăn tối nay");
        vote.setReason("Test concurrency");
        vote.setCreatedByMember(leaderMember);
        vote.setStatus(VoteStatus.OPEN);
        vote.setOpensAt(LocalDateTime.now());
        vote.setClosesAt(LocalDateTime.now().plusDays(1));
        vote.setEligibleVoterCount(2);
        GroupVote savedVote = groupVoteRepository.saveAndFlush(vote);

        GroupVoteOption optionA = new GroupVoteOption();
        optionA.setGroupVote(savedVote);
        optionA.setOptionOrder(1);
        optionA.setOptionLabel("Quán A");
        GroupVoteOption savedOptionA = groupVoteOptionRepository.saveAndFlush(optionA);

        GroupVoteOption optionB = new GroupVoteOption();
        optionB.setGroupVote(savedVote);
        optionB.setOptionOrder(2);
        optionB.setOptionLabel("Quán B");
        groupVoteOptionRepository.saveAndFlush(optionB);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        CastBallotRequest request = CastBallotRequest.builder().optionId(savedOptionA.getGroupVoteOptionId()).build();

        Runnable castByLeader = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                groupVoteService.castBallot(group.getMatchingGroupId(), savedVote.getGroupVoteId(), request, leader.getUserId());
                successCount.incrementAndGet();
            } catch (AppException | InterruptedException ex) {
                failureCount.incrementAndGet();
            }
        };
        Runnable castByMember = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                groupVoteService.castBallot(group.getMatchingGroupId(), savedVote.getGroupVoteId(), request, member.getUserId());
                successCount.incrementAndGet();
            } catch (AppException | InterruptedException ex) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(castByLeader);
        executor.submit(castByMember);
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);

        GroupVote finalVote = groupVoteRepository.findById(savedVote.getGroupVoteId()).orElseThrow();
        assertThat(finalVote.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(finalVote.getWinningOption()).isNotNull();
        assertThat(finalVote.getWinningOption().getGroupVoteOptionId()).isEqualTo(savedOptionA.getGroupVoteOptionId());
        assertThat(finalVote.getClosedAt()).isNotNull();

        long ballotCount = groupVoteBallotRepository.countByGroupVote(finalVote);
        assertThat(ballotCount).isEqualTo(2);
    }

    private User createUser(String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setFullName("Vote Integration " + label);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProvider(AuthProvider.LOCAL);
        return userRepository.saveAndFlush(user);
    }

    private MatchingGroup createGroup(User owner) {
        MatchingGroup group = new MatchingGroup();
        group.setOwner(owner);
        group.setGroupName("Vote Integration Group " + UUID.randomUUID());
        group.setMaxSize(5);
        group.setCurrentSize(2);
        group.setTargetDate(LocalDate.now().plusDays(3));
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
