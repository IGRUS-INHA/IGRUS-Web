package igrus.web.storage.service;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.exception.FileMetadataNotFoundException;
import igrus.web.storage.exception.FileReferenceExistsException;
import igrus.web.storage.exception.S3OperationFailedException;
import igrus.web.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.List;

/**
 * 파일 삭제 서비스.
 *
 * <p>S3 객체 삭제를 먼저 수행하고, 성공 후 DB Soft Delete를 수행한다.
 * 순서 보장이 필수이며, S3 삭제 실패 시 DB 변경을 하지 않는다.</p>
 *
 * <p>참조 무결성: 상위 엔티티에서 참조 중인 파일은 삭제를 거부한다 (409 Conflict).</p>
 * <p>권한 검증: OPERATOR 이상만 삭제 가능 (컨트롤러에서 처리).</p>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class FileDeleteService {

    private final S3Client s3Client;
    private final FileMetadataRepository fileMetadataRepository;
    private final List<FileReferenceChecker> fileReferenceCheckers;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    /**
     * 파일을 삭제한다 (S3 삭제 -> DB Soft Delete).
     *
     * @param objectKey 삭제 대상 Object Key
     * @param userId    삭제 요청자 사용자 ID
     * @throws FileMetadataNotFoundException 파일 메타데이터가 존재하지 않는 경우
     * @throws FileReferenceExistsException  참조 중인 파일인 경우
     * @throws S3OperationFailedException    S3 삭제 실패 시
     */
    public void deleteFile(String objectKey, Long userId) {
        log.info("파일 삭제 요청: objectKey={}, deletedBy={}", objectKey, userId);

        FileMetadata fileMetadata = fileMetadataRepository.findByObjectKeyAndDeletedFalse(objectKey)
                .orElseThrow(() -> new FileMetadataNotFoundException(objectKey));

        // 참조 무결성 체크
        checkFileReferences(objectKey);

        // S3 객체 삭제 (먼저 수행)
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("파일 삭제 실패 (S3): objectKey={}, error={}", objectKey, e.getMessage(), e);
            throw new S3OperationFailedException(e);
        }

        // DB Soft Delete (S3 삭제 성공 후)
        fileMetadata.delete(userId);

        log.info("파일 삭제 완료: objectKey={}, deletedBy={}", objectKey, userId);
    }

    private void checkFileReferences(String objectKey) {
        for (FileReferenceChecker checker : fileReferenceCheckers) {
            if (checker.isReferenced(objectKey)) {
                throw new FileReferenceExistsException(objectKey);
            }
        }
    }
}
