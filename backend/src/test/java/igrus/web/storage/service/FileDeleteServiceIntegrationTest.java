package igrus.web.storage.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.exception.FileReferenceExistsException;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.repository.FileMetadataRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * FileDeleteService 통합 테스트 (S3Client Mock).
 *
 * <p>TC-017: S3 삭제 성공 후 DB Soft Delete</p>
 * <p>TC-018: S3 삭제 실패 시 DB 미변경 500</p>
 * <p>TC-036: 참조 중인 파일 삭제 409</p>
 */
@DisplayName("FileDeleteService 통합 테스트")
@Import(FileDeleteServiceIntegrationTest.TestFileReferenceCheckerConfig.class)
class FileDeleteServiceIntegrationTest extends ServiceIntegrationTestBase {

    /**
     * 테스트용 FileReferenceChecker 설정.
     * 특정 objectKey가 참조 중인 것으로 시뮬레이션한다.
     */
    @TestConfiguration
    static class TestFileReferenceCheckerConfig {
        static volatile String referencedObjectKey = null;

        @Bean
        public FileReferenceChecker testFileReferenceChecker() {
            return objectKey -> objectKey.equals(referencedObjectKey);
        }
    }

    @Autowired
    private FileDeleteService fileDeleteService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private Clock clock;

    private User operatorUser;

    private static final String TEST_OBJECT_KEY = "posts/2026/02/26/delete-test-uuid.png";

    @BeforeEach
    void setUp() {
        setUpBase();
        TestFileReferenceCheckerConfig.referencedObjectKey = null;

        Instant fixedInstant = Instant.parse("2026-02-26T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        reset(s3Client);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        operatorUser = transactionTemplate.execute(status ->
                createAndSaveUser("20231234", "operator@inha.edu", UserRole.OPERATOR));
    }

    private FileMetadata createAndSaveCompletedMetadata(String objectKey) {
        return transactionTemplate.execute(status -> {
            FileMetadata metadata = FileMetadata.create(objectKey, operatorUser.getId(), "test.png", "image/png", 1024L);
            metadata.confirm();
            metadata.complete(Instant.parse("2026-02-26T10:00:00Z"));
            return fileMetadataRepository.save(metadata);
        });
    }

    @DisplayName("TC-017: S3 삭제 성공 후 DB Soft Delete")
    @Test
    void deleteFile_WhenS3Succeeds_SoftDeletesFromDb() {
        // given
        createAndSaveCompletedMetadata(TEST_OBJECT_KEY);

        // when
        fileDeleteService.deleteFile(TEST_OBJECT_KEY, operatorUser.getId());

        // then: findByObjectKeyAndDeletedFalse는 찾지 못함 (soft deleted)
        Optional<FileMetadata> found = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY);
        assertThat(found).isEmpty();
    }

    @DisplayName("TC-018: S3 삭제 실패 시 DB 미변경 및 S3OperationFailedException")
    @Test
    void deleteFile_WhenS3Fails_DoesNotModifyDb() {
        // given
        createAndSaveCompletedMetadata(TEST_OBJECT_KEY);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("S3 delete failed"));

        // when & then
        assertThatThrownBy(() -> fileDeleteService.deleteFile(TEST_OBJECT_KEY, operatorUser.getId()))
                .isInstanceOf(S3OperationFailedException.class);

        // DB가 변경되지 않았는지 확인 (트랜잭션 롤백)
        Optional<FileMetadata> found = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY);
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isFalse();
    }

    @DisplayName("TC-036: 참조 중인 파일 삭제 시 FileReferenceExistsException (409)")
    @Test
    void deleteFile_WhenFileIsReferenced_ThrowsFileReferenceExistsException() {
        // given
        createAndSaveCompletedMetadata(TEST_OBJECT_KEY);
        TestFileReferenceCheckerConfig.referencedObjectKey = TEST_OBJECT_KEY;

        // when & then
        assertThatThrownBy(() -> fileDeleteService.deleteFile(TEST_OBJECT_KEY, operatorUser.getId()))
                .isInstanceOf(FileReferenceExistsException.class);

        // DB가 변경되지 않았는지 확인
        Optional<FileMetadata> found = fileMetadataRepository.findByObjectKeyAndDeletedFalse(TEST_OBJECT_KEY);
        assertThat(found).isPresent();
    }
}
