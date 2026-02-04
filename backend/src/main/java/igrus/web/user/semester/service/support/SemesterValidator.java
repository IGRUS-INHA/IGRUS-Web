package igrus.web.user.semester.service.support;

import igrus.web.user.semester.exception.InvalidSemesterException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학기 유효성 검증.
 */
@Component
@Transactional
public class SemesterValidator {

    /**
     * 연도와 학기의 유효성을 검증합니다.
     *
     * @param year 연도 (2000~2100)
     * @param semester 학기 (1 또는 2)
     * @throws InvalidSemesterException 유효하지 않은 연도 또는 학기인 경우
     */
    public void validateYearAndSemester(int year, int semester) {
        if (year < 2000 || year > 2100) {
            throw new InvalidSemesterException("유효하지 않은 연도입니다: " + year);
        }
        if (semester != 1 && semester != 2) {
            throw new InvalidSemesterException("학기는 1 또는 2만 가능합니다: " + semester);
        }
    }
}
