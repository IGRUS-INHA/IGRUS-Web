package igrus.web.storage.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.dto.DownloadUrlResponse;
import igrus.web.storage.exception.FileMetadataNotFoundException;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DownloadUrlService 통합 테스트 (S3 Presigner Mock).
 *
 * <p>TC-008: 다운로드용 URL 1시간 만료 설정 검증</p>
 * <p>TC-031: 미존재 Object Key 다운로드 404</p>
 * <p>TC-052: S3 SDK 장애 시 500</p>
 */
@DisplayName("DownloadUrlService 통합 테스트")
class DownloadUrlServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private DownloadUrlService downloadUrlService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private Clock clock;

    private User testUser;

    private static final String TEST_OBJECT_KEY = "posts/2026/02/26/download-test-uuid.png";

    @BeforeEach
    void setUp() {
        setUpBase();

        Instant fixedInstant = Instant.parse("2026-02-26T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        reset(s3Presigner);
        PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
        try {
            when(mockPresignedRequest.url()).thenReturn(new URI("https://test-bucket.s3.amazonaws.com/" + TEST_OBJECT_KEY).toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        testUser = transactionTemplate.execute(status ->
                createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE));
    }

    private FileMetadata createAndSaveCompletedMetadata() {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(TEST_OBJECT_KEY, testUser.getId(), "test.png", "image/png", 1024L);
            metadata.confirm();
            metadata.complete(Instant.parse("2026-02-26T10:00:00Z"));
            return fileMetadataRepository.save(metadata);
        });
    }

    @DisplayName("TC-008: 다운로드용 URL 1시간 만료 설정 검증")
    @Test
    void createDownloadUrl_ChecksSignatureDuration1Hour() {
        // given
        createAndSaveCompletedMetadata();

        // when
        downloadUrlService.createDownloadUrl(TEST_OBJECT_KEY, testUser.getId());

        // then
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        GetObjectPresignRequest presignRequest = captor.getValue();
        assertThat(presignRequest.signatureDuration().toSeconds()).isEqualTo(3600);
    }

    @DisplayName("다운로드 URL이 HTTPS로 생성됨")
    @Test
    void createDownloadUrl_ReturnsHttpsUrl() {
        // given
        createAndSaveCompletedMetadata();

        // when
        DownloadUrlResponse response = downloadUrlService.createDownloadUrl(TEST_OBJECT_KEY, testUser.getId());

        // then
        assertThat(response.presignedUrl()).startsWith("https://");
    }

    @DisplayName("TC-031: 미존재 Object Key 다운로드 시 FileMetadataNotFoundException")
    @Test
    void createDownloadUrl_WithNonExistentKey_ThrowsNotFoundException() {
        // when & then
        assertThatThrownBy(() -> downloadUrlService.createDownloadUrl("non-existent-key", testUser.getId()))
                .isInstanceOf(FileMetadataNotFoundException.class);
    }

    @DisplayName("TC-052: S3 SDK 장애 시 S3OperationFailedException")
    @Test
    void createDownloadUrl_WhenS3Fails_ThrowsS3OperationFailedException() {
        // given
        createAndSaveCompletedMetadata();
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.create("S3 connection timeout"));

        // when & then
        assertThatThrownBy(() -> downloadUrlService.createDownloadUrl(TEST_OBJECT_KEY, testUser.getId()))
                .isInstanceOf(S3OperationFailedException.class);
    }
}
