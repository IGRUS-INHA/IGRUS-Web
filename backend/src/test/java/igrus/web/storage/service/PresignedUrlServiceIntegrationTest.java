package igrus.web.storage.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.storage.dto.CreatePresignedUrlResponse;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.exception.UnsupportedContentTypeException;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresignedUrlService 통합 테스트 (S3 Presigner Mock).
 *
 * <p>TC-007: 업로드용 URL 5분 만료 설정 검증</p>
 * <p>TC-014: Presigned URL Content-Type 조건 포함 검증</p>
 * <p>TC-034: Content-Length 상한 조건 포함 검증</p>
 * <p>TC-019: FileMetadata REQUESTED 상태 전이 검증</p>
 * <p>TC-033: Presigned URL HTTPS 검증</p>
 * <p>TC-051: S3 SDK 장애 시 S3OperationFailedException</p>
 * <p>TC-058: 로그에 userId/contentType/fileSize 기록</p>
 * <p>TC-060: Presigned URL 로그 미기록 검증</p>
 * <p>TC-061: 메타데이터 감사 필드 전부 저장</p>
 */
@DisplayName("PresignedUrlService 통합 테스트")
@ExtendWith(OutputCaptureExtension.class)
class PresignedUrlServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private PresignedUrlService presignedUrlService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private Clock clock;

    private User testUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        // Clock Mock 설정
        Instant fixedInstant = Instant.parse("2026-02-26T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        // S3 Presigner Mock 기본 설정
        reset(s3Presigner);
        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        try {
            when(mockPresignedRequest.url()).thenReturn(new URI("https://test-bucket.s3.amazonaws.com/posts/2026/02/26/test-uuid.png?X-Amz-Signature=abc123").toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        // 테스트 사용자 생성
        testUser = transactionTemplate.execute(status -> createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE));
    }

    private CreatePresignedUrlRequest validRequest() {
        return new CreatePresignedUrlRequest("test.png", "image/png", 5242880L, "posts");
    }

    @Nested
    @DisplayName("정상 흐름")
    class HappyPath {

        @DisplayName("TC-007: 업로드용 URL 5분 만료 설정 검증")
        @Test
        void createUploadUrl_ChecksSignatureDuration5Minutes() {
            // when
            presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());

            PutObjectPresignRequest presignRequest = captor.getValue();
            assertThat(presignRequest.signatureDuration().toSeconds()).isEqualTo(300);
        }

        @DisplayName("TC-014: Presigned URL Content-Type 조건 포함 검증")
        @Test
        void createUploadUrl_IncludesContentTypeCondition() {
            // when
            presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());

            PutObjectPresignRequest presignRequest = captor.getValue();
            assertThat(presignRequest.putObjectRequest().contentType()).isEqualTo("image/png");
        }

        @DisplayName("TC-034: Content-Length 상한 조건 포함 검증")
        @Test
        void createUploadUrl_IncludesContentLengthCondition() {
            // when
            presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());

            PutObjectPresignRequest presignRequest = captor.getValue();
            assertThat(presignRequest.putObjectRequest().contentLength()).isEqualTo(5242880L);
        }

        @DisplayName("TC-019: FileMetadata REQUESTED 상태로 DB 저장")
        @Test
        void createUploadUrl_SavesMetadataWithRequestedStatus() {
            // when
            CreatePresignedUrlResponse response = presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            Optional<FileMetadata> savedMetadata = fileMetadataRepository
                    .findByObjectKeyAndDeletedFalse(response.objectKey());

            assertThat(savedMetadata).isPresent();
            assertThat(savedMetadata.get().getStatus()).isEqualTo(FileUploadStatus.REQUESTED);
            assertThat(savedMetadata.get().getUploaderUserId()).isEqualTo(testUser.getId());
        }

        @DisplayName("TC-033: Presigned URL HTTPS 검증")
        @Test
        void createUploadUrl_ReturnsHttpsUrl() {
            // when
            CreatePresignedUrlResponse response = presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            assertThat(response.presignedUrl()).startsWith("https://");
        }

        @DisplayName("TC-061: 메타데이터 감사 필드 전부 저장")
        @Test
        void createUploadUrl_SavesAllAuditFields() {
            // when
            CreatePresignedUrlResponse response = presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            FileMetadata saved = fileMetadataRepository
                    .findByObjectKeyAndDeletedFalse(response.objectKey())
                    .orElseThrow();

            assertThat(saved.getObjectKey()).isNotNull();
            assertThat(saved.getUploaderUserId()).isEqualTo(testUser.getId());
            assertThat(saved.getOriginalFileName()).isEqualTo("test.png");
            assertThat(saved.getContentType()).isEqualTo("image/png");
            assertThat(saved.getFileSize()).isEqualTo(5242880L);
            assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.REQUESTED);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @DisplayName("Presigned URL과 objectKey가 응답에 포함됨")
        @Test
        void createUploadUrl_ReturnsPresignedUrlAndObjectKey() {
            // when
            CreatePresignedUrlResponse response = presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

            // then
            assertThat(response.presignedUrl()).isNotBlank();
            assertThat(response.objectKey()).isNotBlank();
        }
    }

    @DisplayName("TC-058: 로그에 userId/contentType/fileSize 기록")
    @Test
    void createUploadUrl_LogsUserIdContentTypeFileSize(CapturedOutput output) {
        // when
        presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

        // then
        assertThat(output.getAll()).contains("userId=" + testUser.getId());
        assertThat(output.getAll()).contains("contentType=image/png");
        assertThat(output.getAll()).contains("fileSize=5242880");
    }

    @DisplayName("TC-060: Presigned URL 로그 미기록 검증")
    @Test
    void createUploadUrl_DoesNotLogPresignedUrl(CapturedOutput output) {
        // when
        presignedUrlService.createUploadUrl(validRequest(), testUser.getId());

        // then
        assertThat(output.getAll()).doesNotContain("X-Amz-Signature");
        assertThat(output.getAll()).doesNotContain("X-Amz-Credential");
    }

    @Nested
    @DisplayName("예외 상황")
    class ErrorCases {

        @DisplayName("TC-051: S3 SDK 장애 시 S3OperationFailedException")
        @Test
        void createUploadUrl_WhenS3Fails_ThrowsS3OperationFailedException() {
            // given
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenThrow(SdkClientException.create("S3 connection timeout"));

            // when & then
            assertThatThrownBy(() -> presignedUrlService.createUploadUrl(validRequest(), testUser.getId()))
                    .isInstanceOf(S3OperationFailedException.class);
        }

        @DisplayName("허용되지 않은 Content-Type 시 UnsupportedContentTypeException")
        @Test
        void createUploadUrl_WithUnsupportedContentType_ThrowsException() {
            // given
            var request = new CreatePresignedUrlRequest("test.bmp", "image/bmp", 1024L, "posts");

            // when & then
            assertThatThrownBy(() -> presignedUrlService.createUploadUrl(request, testUser.getId()))
                    .isInstanceOf(UnsupportedContentTypeException.class);
        }
    }
}
