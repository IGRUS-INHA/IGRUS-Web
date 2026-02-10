package igrus.web.inquiry.service.support;

import igrus.web.inquiry.domain.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * 문의 저장 실행기.
 *
 * <p>각 저장 시도를 별도 트랜잭션({@code REQUIRES_NEW})으로 격리하여,
 * 문의 번호 중복 등으로 저장 실패 시 호출자의 영속성 컨텍스트가 오염되지 않도록 합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InquiryPersistenceExecutor {

    private final InquiryNumberGenerator inquiryNumberGenerator;

    /**
     * 새로운 트랜잭션에서 문의를 저장합니다.
     *
     * @param inquiryFactory 문의 번호를 받아 엔티티를 생성하는 팩토리
     * @param repository     저장에 사용할 리포지토리
     * @param <T>            문의 엔티티 타입
     * @return 저장된 문의 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T extends Inquiry> T persistInquiry(
            Function<String, T> inquiryFactory,
            JpaRepository<T, Long> repository) {
        String inquiryNumber = inquiryNumberGenerator.generate();
        T inquiry = inquiryFactory.apply(inquiryNumber);
        return repository.save(inquiry);
    }
}
