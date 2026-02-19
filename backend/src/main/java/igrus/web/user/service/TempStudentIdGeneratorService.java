package igrus.web.user.service;

import igrus.web.user.domain.TempStudentIdSequence;
import igrus.web.user.exception.TempStudentIdExhaustedException;
import igrus.web.user.repository.TempStudentIdSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 임시 학번 생성 서비스.
 * 연도별 시퀀스를 관리하여 99YYXXXX 형식의 임시 학번을 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TempStudentIdGeneratorService {

    private final TempStudentIdSequenceRepository sequenceRepository;
    private final Clock clock;

    /**
     * 임시 학번을 생성합니다.
     * 형식: 99 + 연도 2자리 + 순번 4자리 (예: 99260001)
     *
     * @return 생성된 임시 학번 (8자리 문자열)
     * @throws TempStudentIdExhaustedException 해당 연도의 순번이 소진된 경우
     */
    public String generateTempStudentId() {
        int year = LocalDate.now(clock).getYear() % 100;

        TempStudentIdSequence sequence = sequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> sequenceRepository.save(new TempStudentIdSequence(year)));

        int sequenceNumber;
        try {
            sequenceNumber = sequence.getAndIncrement();
        } catch (IllegalStateException e) {
            log.error("임시 학번 시퀀스 소진: year={}", year);
            throw new TempStudentIdExhaustedException();
        }

        String tempStudentId = String.format("99%02d%04d", year, sequenceNumber);
        log.info("임시 학번 생성: {}", tempStudentId);
        return tempStudentId;
    }
}
