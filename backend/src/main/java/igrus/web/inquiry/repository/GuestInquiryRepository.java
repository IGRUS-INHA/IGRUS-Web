package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.GuestInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestInquiryRepository extends JpaRepository<GuestInquiry, Long> {

    // === 비회원 문의 조회 ===
    Optional<GuestInquiry> findByInquiryNumberAndEmail(String inquiryNumber, String email);

    // === 이메일로 문의 조회 ===
    boolean existsByEmail(String email);
}
