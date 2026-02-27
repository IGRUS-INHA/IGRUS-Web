package igrus.web.storage.service;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.ObjectKeyGenerator;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.storage.dto.CreatePresignedUrlResponse;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.exception.UnsupportedContentTypeException;
import igrus.web.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;

/**
 * 업로드용 Presigned URL 생성 서비스.
 *
 * <p>S3 Presigner를 통해 PUT 방식의 Presigned URL을 생성하고,
 * 파일 메타데이터를 REQUESTED 상태로 DB에 저장한다.</p>
 *
 * <p>Presigned URL 자체는 로그에 기록하지 않는다 (보안 위험).</p>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class PresignedUrlService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final S3Presigner s3Presigner;
    private final ObjectKeyGenerator objectKeyGenerator;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${igrus.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${igrus.storage.upload-url-expiration}")
    private long uploadUrlExpiration;

    /**
     * 업로드용 Presigned URL을 생성한다.
     *
     * @param request 업로드 URL 생성 요청
     * @param userId  요청자 사용자 ID
     * @return Presigned URL과 Object Key를 포함한 응답
     * @throws UnsupportedContentTypeException 허용되지 않은 Content-Type인 경우
     * @throws S3OperationFailedException      S3 SDK 장애 시
     */
    public CreatePresignedUrlResponse createUploadUrl(CreatePresignedUrlRequest request, Long userId) {
        validateContentType(request.contentType());

        String objectKey = objectKeyGenerator.generate(request.purpose(), request.contentType());

        log.info("Presigned URL 생성 요청: userId={}, contentType={}, fileSize={}, objectKey={}",
                userId, request.contentType(), request.fileSize(), objectKey);

        String presignedUrl;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(request.contentType())
                    .contentLength(request.fileSize())
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(uploadUrlExpiration))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            presignedUrl = presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: userId={}, objectKey={}, error={}", userId, objectKey, e.getMessage(), e);
            throw new S3OperationFailedException(e);
        }

        FileMetadata fileMetadata = FileMetadata.create(
                objectKey, userId, request.fileName(), request.contentType(), request.fileSize()
        );
        fileMetadataRepository.save(fileMetadata);

        log.info("Presigned URL 생성 완료: userId={}, objectKey={}", userId, objectKey);

        return new CreatePresignedUrlResponse(presignedUrl, objectKey);
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedContentTypeException(contentType);
        }
    }
}
