package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 행사 첨부파일 Repository.
 */
public interface EventAttachmentRepository extends JpaRepository<EventAttachment, Long> {

    /**
     * 행사에 연결된 모든 첨부파일을 조회한다.
     */
    List<EventAttachment> findByEvent(Event event);

    /**
     * 행사에 연결된 모든 첨부파일을 삭제한다.
     */
    void deleteByEvent(Event event);

    /**
     * 해당 objectKey의 파일을 참조하는 활성(deleted=false) 행사의 첨부파일이 존재하는지 확인한다.
     */
    @Query("SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END " +
            "FROM EventAttachment ea " +
            "WHERE ea.fileMetadata.objectKey = :objectKey " +
            "AND ea.event.deleted = false")
    boolean existsByFileMetadataObjectKeyAndEventDeletedFalse(@Param("objectKey") String objectKey);

    /**
     * 행사 ID로 첨부파일을 FileMetadata와 함께 조회한다 (FETCH JOIN).
     */
    @Query("SELECT ea FROM EventAttachment ea " +
            "JOIN FETCH ea.fileMetadata " +
            "WHERE ea.event.id = :eventId")
    List<EventAttachment> findByEventIdWithFileMetadata(@Param("eventId") Long eventId);

    /**
     * 여러 행사 ID에 대해 각 행사의 첫 번째 첨부파일(id 기준)을 한 번에 조회한다 (N+1 방지).
     */
    @Query("SELECT ea FROM EventAttachment ea " +
            "JOIN FETCH ea.fileMetadata " +
            "WHERE ea.event.id IN :eventIds " +
            "AND ea.id IN (" +
            "  SELECT MIN(ea2.id) FROM EventAttachment ea2 " +
            "  WHERE ea2.event.id IN :eventIds " +
            "  GROUP BY ea2.event.id" +
            ")")
    List<EventAttachment> findFirstByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
