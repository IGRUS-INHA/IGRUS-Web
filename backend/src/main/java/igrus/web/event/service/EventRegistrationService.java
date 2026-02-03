package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.AlreadyRegisteredException;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 행사 신청 서비스.
 * 행사 신청 관련 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #registerEvent} - 행사 신청 (정회원 이상)</li>
 *   <li>신청 취소 (신청자 본인)</li>
 *   <li>내 신청 목록 조회</li>
 *   <li>신청자 목록 조회 (운영진/관리자)</li>
 *   <li>신청 승인/거절 - 선발제 (운영진/관리자)</li>
 * </ul>
 *
 * @see EventService 행사 CRUD 관련 기능
 */
@Transactional
@RequiredArgsConstructor
@Service
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;

    /**
     * 행사에 신청합니다.
     *
     * @param eventId 행사 ID
     * @param userId  신청자 ID
     * @return 신청 결과 응답 DTO
     */
    public RegistrationResponse registerEvent(Long eventId, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (정회원 이상)
        if(user.isAssociate()){
            throw new EventAccessDeniedException("준회원은 행사에 신청할 수 없습니다");
        }

        // 4. 이미 신청했는지 확인
        if (eventRegistrationRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new AlreadyRegisteredException("이미 신청한 행사입니다");
        }

        // 5. 행사 상태 확인 (OPEN 상태인지)


        // 6. 신청 기간 확인

        // 7. 정원 확인

        // 8. 신청 생성 및 저장

        // 9. 응답 반환
        return null; // TODO: 구현
    }

}
