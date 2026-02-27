package igrus.web.storage.repository;

import igrus.web.storage.domain.FileMetadata;
import igrus.web.storage.domain.FileUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 파일 메타데이터 Repository.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Object Key로 삭제되지 않은 파일 메타데이터를 조회한다.
     */
    Optional<FileMetadata> findByObjectKeyAndDeletedFalse(String objectKey);

    /**
     * 지정된 상태이고 생성일이 기준 시각 이전인 메타데이터를 조회한다 (스케줄러용).
     */
    List<FileMetadata> findByStatusAndCreatedAtBefore(FileUploadStatus status, Instant threshold);

    /**
     * 삭제되지 않은 메타데이터 중 해당 Object Key가 존재하는지 확인한다.
     */
    boolean existsByObjectKeyAndDeletedFalse(String objectKey);
}
