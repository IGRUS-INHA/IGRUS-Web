package igrus.web.storage.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * FileExpirationService (스케줄러 로직) 통합 테스트.
 *
 * <p>TC-015: REQUESTED 24시간 경과 -> EXPIRED</p>
 * <p>TC-016: REQUESTED 24시간 미경과 유지</p>
 * <p>TC-024: REQUESTED -> EXPIRED (스케줄러)</p>
 */
@DisplayName("FileExpirationService 통합 테스트")
class FileExpirationServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private FileExpirationService fileExpirationService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private Clock clock;

    private User testUser;

    /** 스케줄러 기준 시각: 2026-02-27T10:00:00Z (생성 후 충분한 시간 경과) */
    private static final Instant SCHEDULER_NOW = Instant.parse("2026-02-27T10:00:00Z");

    @BeforeEach
    void setUp() {
        setUpBase();

        when(clock.instant()).thenReturn(SCHEDULER_NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        testUser = transactionTemplate.execute(status ->
                createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE));
    }

    private FileMetadata createAndSaveRequestedMetadataWithCreatedAt(String objectKey, Instant createdAt) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, testUser.getId(), "test.png", "image/png", 1024L);
            FileMetadata saved = fileMetadataRepository.save(metadata);
            // createdAt은 @CreatedDate로 자동 설정되므로 리플렉션으로 덮어쓴다
            entityManager.flush();
            entityManager.createNativeQuery(
                    "UPDATE file_metadata SET file_metadata_created_at = :createdAt WHERE file_metadata_id = :id"
            )
                    .setParameter("createdAt", java.sql.Timestamp.from(createdAt))
                    .setParameter("id", saved.getId())
                    .executeUpdate();
            entityManager.clear();
            return fileMetadataRepository.findById(saved.getId()).orElseThrow();
        });
    }

    @DisplayName("TC-015, TC-024: REQUESTED 24시간 경과 시 EXPIRED로 전환")
    @Test
    void expireStaleRequests_Expired25Hours_TransitionsToExpired() {
        // given: 25시간 전에 생성된 REQUESTED 메타데이터
        Instant createdAt = SCHEDULER_NOW.minus(25, ChronoUnit.HOURS);
        FileMetadata metadata = createAndSaveRequestedMetadataWithCreatedAt(
                "posts/2026/02/26/expired-test.png", createdAt);

        // when
        int count = fileExpirationService.expireStaleRequests();

        // then
        assertThat(count).isEqualTo(1);
        FileMetadata saved = fileMetadataRepository.findById(metadata.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.EXPIRED);
    }

    @DisplayName("TC-016: REQUESTED 24시간 미경과 시 REQUESTED 유지")
    @Test
    void expireStaleRequests_NotYet24Hours_RemainsRequested() {
        // given: 23시간 59분 전에 생성된 REQUESTED 메타데이터
        Instant createdAt = SCHEDULER_NOW.minus(23, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES);
        FileMetadata metadata = createAndSaveRequestedMetadataWithCreatedAt(
                "posts/2026/02/26/not-expired-test.png", createdAt);

        // when
        int count = fileExpirationService.expireStaleRequests();

        // then
        assertThat(count).isEqualTo(0);
        FileMetadata saved = fileMetadataRepository.findById(metadata.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.REQUESTED);
    }

    @DisplayName("COMPLETED/FAILED 상태 레코드는 스케줄러 대상에서 제외")
    @Test
    void expireStaleRequests_CompletedAndFailed_AreExcluded() {
        // given: COMPLETED와 FAILED 상태의 오래된 메타데이터 (25시간 전 생성이지만 이미 상태 전이됨)
        Instant createdAt = SCHEDULER_NOW.minus(25, ChronoUnit.HOURS);

        transactionTemplate.execute(status -> {
            // COMPLETED
            FileMetadata completedMeta = FileMetadata.create(
                    "posts/2026/02/26/completed-test.png", testUser.getId(), "test.png", "image/png", 1024L);
            completedMeta.confirm();
            completedMeta.complete(Instant.now());
            fileMetadataRepository.save(completedMeta);

            // FAILED
            FileMetadata failedMeta = FileMetadata.create(
                    "posts/2026/02/26/failed-test.png", testUser.getId(), "test.png", "image/png", 1024L);
            failedMeta.confirm();
            failedMeta.fail();
            fileMetadataRepository.save(failedMeta);

            return null;
        });

        // when
        int count = fileExpirationService.expireStaleRequests();

        // then: COMPLETED/FAILED는 대상이 아니므로 0건
        assertThat(count).isEqualTo(0);
    }

    @DisplayName("경계값: 정확히 24시간 전 생성 레코드는 EXPIRED로 전환되지 않음")
    @Test
    void expireStaleRequests_Exactly24Hours_RemainsRequested() {
        // given: 정확히 24시간 전에 생성된 REQUESTED 메타데이터
        // findByStatusAndCreatedAtBefore는 createdAt < threshold이므로 정확히 같으면 포함되지 않음
        Instant createdAt = SCHEDULER_NOW.minus(24, ChronoUnit.HOURS);
        FileMetadata metadata = createAndSaveRequestedMetadataWithCreatedAt(
                "posts/2026/02/26/boundary-test.png", createdAt);

        // when
        int count = fileExpirationService.expireStaleRequests();

        // then
        assertThat(count).isEqualTo(0);
        FileMetadata saved = fileMetadataRepository.findById(metadata.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.REQUESTED);
    }
}
