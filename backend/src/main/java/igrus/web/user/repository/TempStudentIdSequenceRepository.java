package igrus.web.user.repository;

import igrus.web.user.domain.TempStudentIdSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TempStudentIdSequenceRepository extends JpaRepository<TempStudentIdSequence, Integer> {

    /**
     * 비관적 쓰기 잠금으로 해당 연도의 시퀀스를 조회합니다.
     * 동시 요청 시 순번 충돌을 방지합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TempStudentIdSequence s WHERE s.year = :year")
    Optional<TempStudentIdSequence> findByYearForUpdate(@Param("year") int year);
}
