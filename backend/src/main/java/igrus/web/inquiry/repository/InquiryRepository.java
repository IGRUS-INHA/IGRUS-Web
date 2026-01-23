package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // === 문의 번호로 조회 ===
    Optional<Inquiry> findByInquiryNumber(String inquiryNumber);

    boolean existsByInquiryNumber(String inquiryNumber);

    // === 상세 조회 (연관 엔티티 포함 - 컬렉션 제외) ===
    @EntityGraph(attributePaths = {"reply", "reply.repliedBy"})
    @Query("SELECT i FROM Inquiry i WHERE i.id = :id")
    Optional<Inquiry> findByIdWithAllRelations(@Param("id") Long id);

    // === 관리자용 목록 조회 (필터링) ===
    @Query("SELECT i FROM Inquiry i WHERE (:type IS NULL OR i.type = :type) AND (:status IS NULL OR i.status = :status)")
    Page<Inquiry> findByFilters(@Param("type") InquiryType type, @Param("status") InquiryStatus status, Pageable pageable);

    // === 오늘 날짜 기준 문의 번호 카운트 (번호 생성용) ===
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.inquiryNumber LIKE :prefix%")
    long countByInquiryNumberPrefix(@Param("prefix") String prefix);

    // === 삭제된 데이터 포함 조회 (소프트 삭제 확인용) ===
    @Query(value = "SELECT COUNT(*) FROM inquiries i WHERE i.inquiries_id = :id", nativeQuery = true)
    long countByIdIncludingDeleted(@Param("id") Long id);

    // === 통계용 쿼리 ===
    long countByStatus(InquiryStatus status);

    long countByType(InquiryType type);

    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") Instant startDate);
}
