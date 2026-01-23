package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.MemberInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberInquiryRepository extends JpaRepository<MemberInquiry, Long> {

    // === 회원 문의 조회 ===
    @EntityGraph(attributePaths = {"user"})
    Page<MemberInquiry> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<MemberInquiry> findByIdAndUserId(Long id, Long userId);

    // === 회원 ID로 문의 존재 여부 확인 ===
    boolean existsByUserId(Long userId);
}
