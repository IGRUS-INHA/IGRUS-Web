package igrus.web.storage.service;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import igrus.web.storage.dto.ConfirmUploadResponse;
import igrus.web.storage.exception.FileMetadataNotFoundException;
import igrus.web.storage.exception.FileOwnershipMismatchException;
import igrus.web.storage.exception.InvalidFileStatusTransitionException;
import igrus.web.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.Clock;
import java.time.Instant;

/**
 * 업로드 완료 확인 서비스.
 *
 * <p>프론트엔드의 업로드 완료 알림을 받아 S3 HEAD 요청으로 파일 존재 여부,
 * Content-Type, Content-Length를 검증한다.</p>
 *
 * <p>상태 전이: REQUESTED -> CONFIRMING -> COMPLETED (성공) 또는 FAILED (실패)</p>
 * <p>COMPLETED 상태에서 재호출 시 멱등하게 성공을 반환한다.</p>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class UploadConfirmService {

    private final S3Client s3Client;
    private final FileMetadataRepository fileMetadataRepository;
    private final Clock clock;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    /**
     * 업로드 완료를 확인한다.
     *
     * @param objectKey 확인 대상 Object Key
     * @param userId    요청자 사용자 ID
     * @return 확인 결과 (COMPLETED 또는 FAILED)
     * @throws FileMetadataNotFoundException      파일 메타데이터가 존재하지 않는 경우
     * @throws FileOwnershipMismatchException      요청자와 업로더가 다른 경우
     * @throws InvalidFileStatusTransitionException FAILED/EXPIRED 상태에서 호출한 경우
     */
    public ConfirmUploadResponse confirmUpload(String objectKey, Long userId) {
        log.info("업로드 완료 확인 요청: objectKey={}, userId={}", objectKey, userId);

        FileMetadata fileMetadata = fileMetadataRepository.findByObjectKeyAndDeletedFalse(objectKey)
                .orElseThrow(() -> new FileMetadataNotFoundException(objectKey));

        validateOwnership(fileMetadata, userId, objectKey);

        // COMPLETED 상태면 멱등하게 성공 반환 (HEAD 검증 생략)
        if (fileMetadata.getStatus() == FileUploadStatus.COMPLETED) {
            log.info("이미 완료된 업로드 (멱등 반환): objectKey={}", objectKey);
            return ConfirmUploadResponse.success(objectKey);
        }

        // FAILED/EXPIRED 상태면 거부
        if (fileMetadata.getStatus() == FileUploadStatus.FAILED
                || fileMetadata.getStatus() == FileUploadStatus.EXPIRED) {
            throw new InvalidFileStatusTransitionException(
                    fileMetadata.getStatus().name(), FileUploadStatus.CONFIRMING.name());
        }

        // REQUESTED -> CONFIRMING
        fileMetadata.confirm();

        // S3 HEAD 검증
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            // Content-Type 검증
            String actualContentType = headResponse.contentType();
            if (!fileMetadata.getContentType().equals(actualContentType)) {
                String reason = String.format("Content-Type 불일치: 예상=%s, 실제=%s",
                        fileMetadata.getContentType(), actualContentType);
                fileMetadata.fail();
                log.warn("업로드 검증 실패: objectKey={}, reason={}", objectKey, reason);
                return ConfirmUploadResponse.failure(objectKey, reason);
            }

            // Content-Length 검증
            Long actualContentLength = headResponse.contentLength();
            if (actualContentLength != null && actualContentLength != fileMetadata.getFileSize()) {
                String reason = String.format("Content-Length 불일치: 예상=%d, 실제=%d",
                        fileMetadata.getFileSize(), actualContentLength);
                fileMetadata.fail();
                log.warn("업로드 검증 실패: objectKey={}, reason={}", objectKey, reason);
                return ConfirmUploadResponse.failure(objectKey, reason);
            }

            // CONFIRMING -> COMPLETED
            fileMetadata.complete(Instant.now(clock));
            log.info("업로드 완료 확인 성공: objectKey={}, userId={}", objectKey, userId);
            return ConfirmUploadResponse.success(objectKey);

        } catch (NoSuchKeyException e) {
            String reason = "S3에 파일이 존재하지 않습니다";
            fileMetadata.fail();
            log.warn("업로드 검증 실패: objectKey={}, reason={}", objectKey, reason);
            return ConfirmUploadResponse.failure(objectKey, reason);
        } catch (Exception e) {
            fileMetadata.fail();
            log.error("업로드 검증 실패 (S3 장애): objectKey={}, error={}", objectKey, e.getMessage(), e);
            return ConfirmUploadResponse.failure(objectKey, "S3 검증 중 오류가 발생했습니다");
        }
    }

    private void validateOwnership(FileMetadata fileMetadata, Long userId, String objectKey) {
        if (!fileMetadata.getUploaderUserId().equals(userId)) {
            throw new FileOwnershipMismatchException(objectKey);
        }
    }
}
