package igrus.web.event.service;

import igrus.web.event.exception.InvalidEventDateException;

import java.time.Instant;

/**
 * 행사 날짜 검증 유틸리티.
 *
 * <p>2축 상태 모델 날짜 검증:
 * <ul>
 *   <li>regStart < regEnd</li>
 *   <li>regStart < eventStart (등록 시작은 행사 시작 전)</li>
 *   <li>regEnd <= eventEnd (등록 마감은 행사 종료 이전 또는 동일)</li>
 *   <li>eventStart <= eventEnd</li>
 * </ul>
 */
public final class EventDateValidator {

    private EventDateValidator() {
    }

    /**
     * 행사 날짜 간의 논리적 순서를 검증한다.
     *
     * @param eventStart 행사 시작일
     * @param eventEnd   행사 종료일
     * @param regStart   신청 시작일
     * @param regEnd     신청 마감일
     * @throws InvalidEventDateException 날짜 조건이 맞지 않을 경우
     */
    public static void validate(Instant eventStart, Instant eventEnd,
                                Instant regStart, Instant regEnd) {
        if (!regStart.isBefore(regEnd)) {
            throw new InvalidEventDateException("신청 마감일은 신청 시작일 이후여야 합니다");
        }

        if (!regStart.isBefore(eventStart)) {
            throw new InvalidEventDateException("신청 시작일은 행사 시작일 이전이어야 합니다");
        }

        if (regEnd.isAfter(eventEnd)) {
            throw new InvalidEventDateException("신청 마감일은 행사 종료일 이전이거나 같아야 합니다");
        }

        if (eventStart.isAfter(eventEnd)) {
            throw new InvalidEventDateException("행사 종료일은 시작일 이후이거나 같아야 합니다");
        }
    }
}
