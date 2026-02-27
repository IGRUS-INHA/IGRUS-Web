package igrus.web.storage.domain;

import igrus.web.storage.exception.InvalidFileStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FileMetadata 상태 전이 단위 테스트.
 *
 * <p>정상 전이: REQUESTED->CONFIRMING, CONFIRMING->COMPLETED, CONFIRMING->FAILED, REQUESTED->EXPIRED</p>
 * <p>금지 전이: COMPLETED->CONFIRMING, FAILED->COMPLETED, EXPIRED->CONFIRMING 등</p>
 * <p>TC-025, TC-026, TC-027 관련</p>
 */
@DisplayName("FileMetadata 상태 전이 단위 테스트")
class FileMetadataTest {

    private FileMetadata createRequestedMetadata() {
        return FileMetadata.create(
                "posts/2026/02/26/test-uuid.png",
                1L,
                "test.png",
                "image/png",
                1024L
        );
    }

    @Nested
    @DisplayName("정상 전이")
    class ValidTransitions {

        @DisplayName("REQUESTED -> CONFIRMING 전이 성공")
        @Test
        void confirm_FromRequested_TransitionsToConfirming() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            assertThat(metadata.getStatus()).isEqualTo(FileUploadStatus.CONFIRMING);
        }

        @DisplayName("CONFIRMING -> COMPLETED 전이 성공")
        @Test
        void complete_FromConfirming_TransitionsToCompleted() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();

            Instant completedAt = Instant.now();
            metadata.complete(completedAt);

            assertThat(metadata.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
            assertThat(metadata.getCompletedAt()).isEqualTo(completedAt);
        }

        @DisplayName("CONFIRMING -> FAILED 전이 성공")
        @Test
        void fail_FromConfirming_TransitionsToFailed() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            metadata.fail();
            assertThat(metadata.getStatus()).isEqualTo(FileUploadStatus.FAILED);
        }

        @DisplayName("REQUESTED -> EXPIRED 전이 성공")
        @Test
        void expire_FromRequested_TransitionsToExpired() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.expire();
            assertThat(metadata.getStatus()).isEqualTo(FileUploadStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("금지된 전이")
    class InvalidTransitions {

        @DisplayName("TC-025: COMPLETED -> CONFIRMING 전이 거부")
        @Test
        void confirm_FromCompleted_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            metadata.complete(Instant.now());

            assertThatThrownBy(metadata::confirm)
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("TC-026: FAILED -> COMPLETED 전이 거부")
        @Test
        void complete_FromFailed_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            metadata.fail();

            assertThatThrownBy(() -> metadata.complete(Instant.now()))
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("TC-027: EXPIRED -> CONFIRMING 전이 거부")
        @Test
        void confirm_FromExpired_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.expire();

            assertThatThrownBy(metadata::confirm)
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("COMPLETED -> EXPIRED 전이 거부")
        @Test
        void expire_FromCompleted_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            metadata.complete(Instant.now());

            assertThatThrownBy(metadata::expire)
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("FAILED -> CONFIRMING 전이 거부")
        @Test
        void confirm_FromFailed_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();
            metadata.fail();

            assertThatThrownBy(metadata::confirm)
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("EXPIRED -> COMPLETED 전이 거부")
        @Test
        void complete_FromExpired_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.expire();

            assertThatThrownBy(() -> metadata.complete(Instant.now()))
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }

        @DisplayName("CONFIRMING -> EXPIRED 전이 거부")
        @Test
        void expire_FromConfirming_ThrowsException() {
            FileMetadata metadata = createRequestedMetadata();
            metadata.confirm();

            assertThatThrownBy(metadata::expire)
                    .isInstanceOf(InvalidFileStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("생성 검증")
    class Creation {

        @DisplayName("create() 시 REQUESTED 상태로 생성됨")
        @Test
        void create_ReturnsRequestedStatus() {
            FileMetadata metadata = createRequestedMetadata();

            assertThat(metadata.getStatus()).isEqualTo(FileUploadStatus.REQUESTED);
            assertThat(metadata.getObjectKey()).isEqualTo("posts/2026/02/26/test-uuid.png");
            assertThat(metadata.getUploaderUserId()).isEqualTo(1L);
            assertThat(metadata.getOriginalFileName()).isEqualTo("test.png");
            assertThat(metadata.getContentType()).isEqualTo("image/png");
            assertThat(metadata.getFileSize()).isEqualTo(1024L);
            assertThat(metadata.getCompletedAt()).isNull();
        }
    }
}
