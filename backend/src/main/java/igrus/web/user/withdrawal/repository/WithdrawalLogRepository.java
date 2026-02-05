package igrus.web.user.withdrawal.repository;

import igrus.web.user.withdrawal.domain.WithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalLogRepository extends JpaRepository<WithdrawalLog, Long> {
}
