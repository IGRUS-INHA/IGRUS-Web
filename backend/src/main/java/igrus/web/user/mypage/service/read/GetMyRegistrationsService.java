package igrus.web.user.mypage.service.read;

import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.repository.EventRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyRegistrationsService {

    private final EventRegistrationRepository eventRegistrationRepository;

    public List<MyRegistrationResponse> getMyRegistrations(Long userId) {
        log.info("내 행사 신청 목록 조회 - userId: {}", userId);

        return eventRegistrationRepository.findByUserId(userId)
                .stream()
                .map(MyRegistrationResponse::from)
                .toList();
    }
}
