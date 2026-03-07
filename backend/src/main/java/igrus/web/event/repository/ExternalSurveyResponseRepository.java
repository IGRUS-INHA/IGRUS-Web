package igrus.web.event.repository;

import igrus.web.event.domain.ExternalSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 외부인 설문 응답 Repository. (DECISION-04: 옵션 B - 별도 테이블)
 */
public interface ExternalSurveyResponseRepository extends JpaRepository<ExternalSurveyResponse, Long> {
}
