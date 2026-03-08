package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 행사 등록 상태 갱신 헬퍼.
 * 신청자 수 변경(증가/감소) 후 행사의 등록 상태를 업데이트하는 공통 로직을 제공합니다.
 *
 * <p>{@link EventRegistrationService}와 {@link ExternalEventRegistrationService}에서
 * 중복되던 상태 갱신 로직을 통합합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EventStatusHelper {

    private final EventRepository eventRepository;

    /**
     * 신청자 수 증가 후 등록 상태를 업데이트합니다.
     * 정원이 찼으면 CLOSED 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    public void updateEventStatusAfterIncrement(Long eventId) {
        Event event = eventRepository.findByIdAndNotDeleted(eventId).orElse(null);
        if (event == null) {
            log.warn("행사 상태 갱신 실패: 원자적 UPDATE 이후 행사를 찾을 수 없음. eventId={}", eventId);
            return;
        }
        if (event.isFull()) {
            event.closeRegistrationByCapacity();
        }
    }

    /**
     * 신청자 수 감소 후 등록 상태를 업데이트합니다.
     * 정원 마감 상태에서 자리가 생기면 OPEN 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    public void updateEventStatusAfterDecrement(Long eventId) {
        Event event = eventRepository.findByIdAndNotDeleted(eventId).orElse(null);
        if (event == null) {
            log.warn("행사 상태 갱신 실패: 원자적 UPDATE 이후 행사를 찾을 수 없음. eventId={}", eventId);
            return;
        }
        event.reopenIfNeeded(Instant.now());
    }
}
