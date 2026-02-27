package igrus.web.storage.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.dto.ConfirmUploadResponse;
import igrus.web.storage.exception.FileMetadataNotFoundException;
import igrus.web.storage.exception.FileOwnershipMismatchException;
import igrus.web.storage.exception.InvalidFileStatusTransitionException;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UploadConfirmService 통합 테스트 (S3Client Mock).
 *
 * <p>TC-011: S3 HEAD 성공 시 COMPLETED 전이</p>
 * <p>TC-012: 존재하지 않는 Object Key 완료 알림 실패</p>
 * <p>TC-020: REQUESTED -> CONFIRMING -> COMPLETED</p>
 * <p>TC-021: CONFIRMING -> FAILED (파일 미존재)</p>
 * <p>TC-022: CONFIRMING -> FAILED (Content-Type 불일치)</p>
 * <p>TC-023: CONFIRMING -> FAILED (Content-Length 불일치)</p>
 * <p>TC-025: COMPLETED 상태 멱등</p>
 * <p>TC-026: FAILED -> COMPLETED 전이 거부</p>
 * <p>TC-027: EXPIRED -> UPLOADING 전이 거부</p>
 * <p>TC-028: COMPLETED 상태 멱등 성공</p>
 * <p>TC-029: 다른 사용자 업로드 완료 알림 403</p>
 * <p>TC-053: S3 HEAD 장애 시 FAILED</p>
 * <p>TC-056: 완료 알림 멱등 3회 호출</p>
 * <p>TC-059: 완료 확인 로그에 objectKey/userId 기록</p>
 * <p>TC-062: completedAt 필드 기록 확인</p>
 */
@DisplayName("UploadConfirmService 통합 테스트")
@ExtendWith(OutputCaptureExtension.class)
class UploadConfirmServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private UploadConfirmService uploadConfirmService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private Clock clock;

    private User testUser;
    private User otherUser;

    private static final String TEST_OBJECT_KEY = "posts/2026/02/26/test-uuid.png";

    @BeforeEach
    void setUp() {
        setUpBase();

        Instant fixedInstant = Instant.parse("2026-02-26T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        reset(s3Client);

        testUser = transactionTemplate.execute(status ->
                createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE));
        otherUser = transactionTemplate.execute(status ->
                createAndSaveUser("20235678", "other@inha.edu", UserRole.ASSOCIATE));
    }

    private FileMetadata createAndSaveRequestedMetadata(String objectKey, Long userId) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, userId, "test.png", "image/png", 1024L);
            return fileMetadataRepository.save(metadata);
        });
    }

    private FileMetadata createAndSaveCompletedMetadata(String objectKey, Long userId) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, userId, "test.png", "image/png", 1024L);
            metadata.confirm();
            metadata.complete(Instant.parse("2026-02-26T10:00:00Z"));
            return fileMetadataRepository.save(metadata);
        });
    }

    private FileMetadata createAndSaveFailedMetadata(String objectKey, Long userId) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, userId, "test.png", "image/png", 1024L);
            metadata.confirm();
            metadata.fail();
            return fileMetadataRepository.save(metadata);
        });
    }

    private FileMetadata createAndSaveExpiredMetadata(String objectKey, Long userId) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, userId, "test.png", "image/png", 1024L);
            metadata.expire();
            return fileMetadataRepository.save(metadata);
        });
    }

    private void stubHeadObjectSuccess(String contentType, long contentLength) {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
    }

    @Nested
    @DisplayName("정상 흐름")
    class HappyPath {

        @DisplayName("TC-011, TC-020: S3 HEAD 성공 시 REQUESTED -> CONFIRMING -> COMPLETED 전이")
        @Test
        void confirmUpload_WithValidHead_CompletesTransition() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            stubHeadObjectSuccess("image/png", 1024L);

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.objectKey()).isEqualTo(TEST_OBJECT_KEY);
            assertThat(response.reason()).isNull();

            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        }

        @DisplayName("TC-062: completedAt 필드 기록 확인")
        @Test
        void confirmUpload_RecordsCompletedAt() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            stubHeadObjectSuccess("image/png", 1024L);

            // when
            uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getCompletedAt()).isNotNull();
            // completedAt은 mock clock 기반 (2026-02-26T10:00:00Z)
            assertThat(saved.getCompletedAt()).isEqualTo(Instant.parse("2026-02-26T10:00:00Z"));
        }

        @DisplayName("TC-028: COMPLETED 상태 멱등 성공 (HEAD 생략)")
        @Test
        void confirmUpload_WhenAlreadyCompleted_ReturnsIdempotentSuccess() {
            // given
            createAndSaveCompletedMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.objectKey()).isEqualTo(TEST_OBJECT_KEY);
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }

        @DisplayName("TC-056: 완료 알림 멱등 3회 호출")
        @Test
        void confirmUpload_Idempotent3Times_AllSucceed() {
            // given
            createAndSaveCompletedMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when & then
            for (int i = 0; i < 3; i++) {
                ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());
                assertThat(response.status()).isEqualTo("COMPLETED");
            }
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }
    }

    @Nested
    @DisplayName("HEAD 검증 실패")
    class HeadVerificationFailure {

        @DisplayName("TC-012, TC-021: S3에 파일이 존재하지 않을 때 FAILED 전이")
        @Test
        void confirmUpload_WhenFileNotInS3_TransitionsToFailed() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().message("Not found").build());

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("FAILED");
            assertThat(response.reason()).isNotBlank();

            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.FAILED);
        }

        @DisplayName("TC-022: Content-Type 불일치 시 FAILED 전이")
        @Test
        void confirmUpload_WithContentTypeMismatch_TransitionsToFailed() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            stubHeadObjectSuccess("image/jpeg", 1024L); // image/png 기대, image/jpeg 실제

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("FAILED");
            assertThat(response.reason()).contains("Content-Type");

            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.FAILED);
        }

        @DisplayName("TC-023: Content-Length 불일치 시 FAILED 전이")
        @Test
        void confirmUpload_WithContentLengthMismatch_TransitionsToFailed() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            stubHeadObjectSuccess("image/png", 2048L); // 1024 기대, 2048 실제

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("FAILED");
            assertThat(response.reason()).contains("Content-Length");

            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.FAILED);
        }

        @DisplayName("TC-053: S3 HEAD 장애 시 FAILED 전이")
        @Test
        void confirmUpload_WhenS3HeadFails_TransitionsToFailed() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(SdkClientException.create("S3 connection timeout"));

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("FAILED");

            FileMetadata saved = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("금지된 전이")
    class ForbiddenTransitions {

        @DisplayName("TC-025: COMPLETED 상태 재호출 시 멱등 반환 (전이 거부 아님)")
        @Test
        void confirmUpload_FromCompleted_ReturnsIdempotentSuccess() {
            // given
            createAndSaveCompletedMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when
            ConfirmUploadResponse response = uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

            // then
            assertThat(response.status()).isEqualTo("COMPLETED");
        }

        @DisplayName("TC-026: FAILED 상태에서 호출 시 전이 거부")
        @Test
        void confirmUpload_FromFailed_ThrowsInvalidTransition() {
            // given
            createAndSaveFailedMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when & then
            assertThatThrownBy(() -> uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId()))
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("TC-027: EXPIRED 상태에서 호출 시 전이 거부")
        @Test
        void confirmUpload_FromExpired_ThrowsInvalidTransition() {
            // given
            createAndSaveExpiredMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when & then
            assertThatThrownBy(() -> uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId()))
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("보안 검증")
    class SecurityVerification {

        @DisplayName("TC-029: 다른 사용자 업로드 완료 알림 시 403")
        @Test
        void confirmUpload_WithDifferentUser_ThrowsOwnershipMismatch() {
            // given
            createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());

            // when & then
            assertThatThrownBy(() -> uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, otherUser.getId()))
                    .isInstanceOf(FileOwnershipMismatchException.class);
        }

        @DisplayName("존재하지 않는 objectKey 시 FileMetadataNotFoundException")
        @Test
        void confirmUpload_WithNonExistentKey_ThrowsNotFoundException() {
            // when & then
            assertThatThrownBy(() -> uploadConfirmService.confirmUpload("non-existent-key", testUser.getId()))
                    .isInstanceOf(FileMetadataNotFoundException.class);
        }
    }

    @DisplayName("TC-059: 완료 확인 로그에 objectKey/userId 기록")
    @Test
    void confirmUpload_LogsObjectKeyAndUserId(CapturedOutput output) {
        // given
        createAndSaveRequestedMetadata(TEST_OBJECT_KEY, testUser.getId());
        stubHeadObjectSuccess("image/png", 1024L);

        // when
        uploadConfirmService.confirmUpload(TEST_OBJECT_KEY, testUser.getId());

        // then
        assertThat(output.getAll()).contains("objectKey=" + TEST_OBJECT_KEY);
        assertThat(output.getAll()).contains("userId=" + testUser.getId());
    }
}
