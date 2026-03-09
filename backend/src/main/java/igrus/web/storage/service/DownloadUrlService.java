package igrus.web.storage.service;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.dto.DownloadUrlResponse;
import igrus.web.storage.exception.FileMetadataNotFoundException;
import igrus.web.storage.exception.InvalidFileStatusTransitionException;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

/**
 * 다운로드용 Presigned URL 생성 서비스.
 *
 * <p>COMPLETED 상태의 파일에 대해 S3 Presigner를 통해
 * GET 방식의 Presigned URL을 생성한다. 만료 시간은 1시간.</p>
 */
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DownloadUrlService {

    private final S3Presigner s3Presigner;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${app.storage.download-url-expiration}")
    private long downloadUrlExpiration;

    /**
     * 다운로드용 Presigned URL을 생성한다.
     *
     * @param objectKey 다운로드 대상 Object Key
     * @param userId    요청자 사용자 ID
     * @return 다운로드용 Presigned URL 응답
     * @throws FileMetadataNotFoundException COMPLETED 상태의 파일이 존재하지 않는 경우
     * @throws S3OperationFailedException    S3 SDK 장애 시
     */
    public DownloadUrlResponse createDownloadUrl(String objectKey, Long userId) {
        log.info("다운로드 URL 생성 요청: objectKey={}, userId={}", objectKey, userId);
        DownloadUrlResponse response = generatePresignedDownloadUrl(objectKey);
        log.info("다운로드 URL 생성 완료: objectKey={}, userId={}", objectKey, userId);
        return response;
    }

    /**
     * 공개 다운로드용 Presigned URL을 생성한다. (인증 불필요)
     * 호출 전에 objectKey의 접근 권한은 호출자(서비스 레이어)가 검증해야 한다.
     *
     * @param objectKey 다운로드 대상 Object Key
     * @return 다운로드용 Presigned URL 응답
     * @throws FileMetadataNotFoundException       COMPLETED 상태의 파일이 존재하지 않는 경우
     * @throws InvalidFileStatusTransitionException 파일이 COMPLETED 상태가 아닌 경우
     * @throws S3OperationFailedException           S3 SDK 장애 시
     */
    public DownloadUrlResponse createPublicDownloadUrl(String objectKey) {
        log.info("공개 다운로드 URL 생성 요청: objectKey={}", objectKey);
        DownloadUrlResponse response = generatePresignedDownloadUrl(objectKey);
        log.info("공개 다운로드 URL 생성 완료: objectKey={}", objectKey);
        return response;
    }

    private DownloadUrlResponse generatePresignedDownloadUrl(String objectKey) {
        FileMetadata fileMetadata = fileMetadataRepository.findByObjectKeyAndDeletedFalse(objectKey)
                .orElseThrow(() -> new FileMetadataNotFoundException(objectKey));

        if (fileMetadata.getStatus() != FileUploadStatus.COMPLETED) {
            throw new InvalidFileStatusTransitionException(
                    fileMetadata.getStatus().name(), "DOWNLOAD");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(downloadUrlExpiration))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return new DownloadUrlResponse(presignedRequest.url().toString());
        } catch (Exception e) {
            log.error("다운로드 URL 생성 실패: objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new S3OperationFailedException(e);
        }
    }
}
