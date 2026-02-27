package igrus.web.storage.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.storage.exception.InvalidFileStatusTransitionException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 파일 메타데이터 엔티티.
 * S3에 업로드된 파일의 메타데이터를 관리한다.
 */
@Entity
@Table(name = "file_metadata")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "file_metadata_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "file_metadata_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "file_metadata_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "file_metadata_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "file_metadata_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "file_metadata_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "file_metadata_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadata extends SoftDeletableEntity {

    /** 파일 메타데이터 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_metadata_id")
    private Long id;

    /** S3 Object Key (유일, 예: posts/2026/02/26/{UUID}.png) */
    @Column(name = "file_metadata_object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    /** 업로드 요청자 사용자 ID */
    @Column(name = "file_metadata_uploader_user_id", nullable = false)
    private Long uploaderUserId;

    /** 원본 파일명 */
    @Column(name = "file_metadata_original_file_name", nullable = false)
    private String originalFileName;

    /** Content-Type (MIME 타입) */
    @Column(name = "file_metadata_content_type", nullable = false, length = 100)
    private String contentType;

    /** 파일 크기 (bytes) */
    @Column(name = "file_metadata_file_size", nullable = false)
    private long fileSize;

    /** 업로드 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_metadata_status", nullable = false, length = 20)
    private FileUploadStatus status;

    /** 업로드 완료 시각 */
    @Column(name = "file_metadata_completed_at")
    private Instant completedAt;

    private FileMetadata(String objectKey, Long uploaderUserId, String originalFileName,
                         String contentType, long fileSize) {
        this.objectKey = objectKey;
        this.uploaderUserId = uploaderUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = FileUploadStatus.REQUESTED;
    }

    /**
     * FileMetadata 인스턴스를 생성한다 (status = REQUESTED).
     */
    public static FileMetadata create(String objectKey, Long uploaderUserId,
                                      String originalFileName, String contentType, long fileSize) {
        return new FileMetadata(objectKey, uploaderUserId, originalFileName, contentType, fileSize);
    }

    /**
     * REQUESTED -> CONFIRMING 상태 전이.
     *
     * @throws InvalidFileStatusTransitionException REQUESTED 상태가 아닌 경우
     */
    public void confirm() {
        if (this.status != FileUploadStatus.REQUESTED) {
            throw new InvalidFileStatusTransitionException(this.status.name(), FileUploadStatus.CONFIRMING.name());
        }
        this.status = FileUploadStatus.CONFIRMING;
    }

    /**
     * CONFIRMING -> COMPLETED 상태 전이.
     *
     * @param completedAt 완료 시각
     * @throws InvalidFileStatusTransitionException CONFIRMING 상태가 아닌 경우
     */
    public void complete(Instant completedAt) {
        if (this.status != FileUploadStatus.CONFIRMING) {
            throw new InvalidFileStatusTransitionException(this.status.name(), FileUploadStatus.COMPLETED.name());
        }
        this.status = FileUploadStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    /**
     * CONFIRMING -> FAILED 상태 전이.
     *
     * @throws InvalidFileStatusTransitionException CONFIRMING 상태가 아닌 경우
     */
    public void fail() {
        if (this.status != FileUploadStatus.CONFIRMING) {
            throw new InvalidFileStatusTransitionException(this.status.name(), FileUploadStatus.FAILED.name());
        }
        this.status = FileUploadStatus.FAILED;
    }

    /**
     * REQUESTED -> EXPIRED 상태 전이 (스케줄러에 의한 만료 처리).
     *
     * @throws InvalidFileStatusTransitionException REQUESTED 상태가 아닌 경우
     */
    public void expire() {
        if (this.status != FileUploadStatus.REQUESTED) {
            throw new InvalidFileStatusTransitionException(this.status.name(), FileUploadStatus.EXPIRED.name());
        }
        this.status = FileUploadStatus.EXPIRED;
    }
}
