package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.AdminEditUserInfoRequest;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminEditUserInfoService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void editUserInfo(Long targetUserId, AdminEditUserInfoRequest request, Long currentUserId) {
        log.info("관리자 사용자 정보 수정 요청 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        updateStudentId(targetUser, request);
        boolean emailChanged = updateEmail(targetUser, request);
        updateProfile(targetUser, request);
        updateMotivation(targetUser, request);
        updateSurveyInfo(targetUser, request);

        if (emailChanged) {
            emailVerificationRepository.deleteByEmail(targetUser.getEmail());
        }

        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                targetUserId, currentUserId, AccountChangeType.ADMIN_INFO_EDIT,
                "INFO", "INFO",
                "관리자에 의한 사용자 정보 수정"
        ));

        log.info("관리자 사용자 정보 수정 완료 - targetUserId: {}, performedBy: {}", targetUserId, currentUserId);
    }

    private void updateStudentId(User targetUser, AdminEditUserInfoRequest request) {
        if (request.studentId() == null) {
            return;
        }

        if (request.studentId().equals(targetUser.getStudentId())) {
            return;
        }

        if (userRepository.existsByStudentId(request.studentId())) {
            throw new DuplicateStudentIdException();
        }

        targetUser.updateStudentId(request.studentId());
    }

    private boolean updateEmail(User targetUser, AdminEditUserInfoRequest request) {
        if (request.email() == null) {
            return false;
        }

        String currentEmail = targetUser.getEmail();
        if (request.email().equals(currentEmail)) {
            return false;
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        String previousEmail = targetUser.getEmail();
        targetUser.updateEmail(request.email());
        emailVerificationRepository.deleteByEmail(previousEmail);
        return true;
    }

    private void updateProfile(User targetUser, AdminEditUserInfoRequest request) {
        if (request.name() == null && request.phoneNumber() == null && request.department() == null
                && request.gender() == null && request.grade() == null && request.enrollmentStatus() == null) {
            return;
        }

        if (request.phoneNumber() != null
                && !request.phoneNumber().equals(targetUser.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicatePhoneNumberException(request.phoneNumber());
        }

        targetUser.updateProfile(
                request.name() != null ? request.name() : targetUser.getName(),
                request.phoneNumber() != null ? request.phoneNumber() : targetUser.getPhoneNumber(),
                request.department() != null ? request.department() : targetUser.getDepartment(),
                request.gender() != null ? request.gender() : targetUser.getGender(),
                request.grade() != null ? request.grade() : targetUser.getGrade(),
                request.enrollmentStatus() != null ? request.enrollmentStatus() : targetUser.getEnrollmentStatus()
        );
    }

    private void updateMotivation(User targetUser, AdminEditUserInfoRequest request) {
        if (request.motivation() != null) {
            targetUser.updateMotivation(request.motivation());
        }
    }

    private void updateSurveyInfo(User targetUser, AdminEditUserInfoRequest request) {
        if (request.wishes() == null && request.interests() == null && request.customInterest() == null
                && request.joinRoute() == null && request.customJoinRoute() == null) {
            return;
        }

        targetUser.updateSurveyInfo(
                request.wishes() != null ? request.wishes() : targetUser.getWishes(),
                request.interests() != null ? request.interests() : targetUser.getInterests(),
                request.customInterest() != null ? request.customInterest() : targetUser.getCustomInterest(),
                request.joinRoute() != null ? request.joinRoute() : targetUser.getJoinRoute(),
                request.customJoinRoute() != null ? request.customJoinRoute() : targetUser.getCustomJoinRoute()
        );
    }
}
