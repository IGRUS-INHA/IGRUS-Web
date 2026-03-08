package igrus.web.event.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.event.exception.EventNotEditableException;
import igrus.web.event.exception.InvalidEventCapacityException;
import igrus.web.event.exception.InvalidEventStateTransitionException;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 행사 엔티티.
 * 동아리에서 진행하는 행사 정보를 관리합니다.
 *
 * <p>3축 상태 모델:</p>
 * <ul>
 *   <li>축 1: {@link #visibility} — 공개 상태 (UNPUBLISHED, PUBLISHED)</li>
 *   <li>축 2: {@link #registrationStatus} — 등록(모집) 상태 (NOT_STARTED, OPEN, CLOSED)</li>
 *   <li>축 3: {@link #eventStatus} — 행사 진행 상태 (UPCOMING, ONGOING, COMPLETED, CANCELED)</li>
 * </ul>
 */
@Entity
@Table(name = "events")
@SQLRestriction("event_deleted = false")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "event_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "event_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "event_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "event_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "event_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "event_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "event_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends SoftDeletableEntity {

    /** 행사 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    /** 행사 작성자 (운영진) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_user_id", nullable = false)
    private User user;

    /** 행사 제목 */
    @Column(name = "event_title", nullable = false, length = 100)
    private String title;

    /** 행사 설명 */
    @Column(name = "event_description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** 행사 장소 */
    @Column(name = "event_location", nullable = false, length = 200)
    private String location;

    /** 행사 시작 일시 */
    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    /** 행사 종료 일시 */
    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    /** 신청 시작일 */
    @Column(name = "event_registration_start_at", nullable = false)
    private Instant registrationStartAt;

    /** 신청 마감일 */
    @Column(name = "event_registration_end_at", nullable = false)
    private Instant registrationEndAt;

    /** 정원 (최대 참가 인원) */
    @Column(name = "event_capacity", nullable = false)
    private Integer capacity;

    /** 현재 신청자 수 */
    @Column(name = "event_current_count", nullable = false)
    private int currentCount = 0;

    /** 축 1: 공개 상태 (UNPUBLISHED / PUBLISHED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_visibility", nullable = false, length = 20)
    private EventVisibility visibility = EventVisibility.UNPUBLISHED;

    /** 축 3: 행사 진행 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus eventStatus = EventStatus.UPCOMING;

    /** 축 2: 등록(모집) 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus = RegistrationStatus.NOT_STARTED;

    /** 마감 사유 (registrationStatus == CLOSED일 때만 값 존재) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_close_reason", length = 20)
    private EventCloseReason closeReason;

    /** 신청 방식 (선착순/선발제) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_registration_type", nullable = false, length = 20)
    private EventRegistrationType registrationType;

    /** 연결된 설문 ID (nullable, surveys.survey_id FK 참조) */
    @Column(name = "event_survey_id")
    private Long surveyId;

    /** 첨부 이미지 목록 (최대 5개) */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImage> images = new ArrayList<>();

    // === 정적 팩토리 메서드 ===

    /**
     * 행사를 생성합니다.
     * 초기 상태: visibility=UNPUBLISHED, registrationStatus=NOT_STARTED, eventStatus=UPCOMING, currentCount=0
     * DB 기본값은 PUBLISHED이지만, 신규 생성 시에는 UNPUBLISHED로 명시 설정합니다.
     *
     * @throws InvalidEventCapacityException 정원이 1 미만인 경우
     */
    public static Event create(User user, String title, String description, String location,
                               Instant eventStartAt, Instant eventEndAt,
                               Instant registrationStartAt, Instant registrationEndAt,
                               Integer capacity, EventRegistrationType registrationType,
                               Long surveyId) {
        validateCapacity(capacity);

        Event event = new Event();
        event.user = user;
        event.title = title;
        event.description = description;
        event.location = location;
        event.eventStartAt = eventStartAt;
        event.eventEndAt = eventEndAt;
        event.registrationStartAt = registrationStartAt;
        event.registrationEndAt = registrationEndAt;
        event.capacity = capacity;
        event.currentCount = 0;
        event.visibility = EventVisibility.UNPUBLISHED;
        event.registrationStatus = RegistrationStatus.NOT_STARTED;
        event.eventStatus = EventStatus.UPCOMING;
        event.registrationType = registrationType;
        event.surveyId = surveyId;
        return event;
    }

    private static void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new InvalidEventCapacityException(capacity);
        }
    }

    // === 축 1: 공개 상태 변경 메서드 ===

    /**
     * 행사를 공개합니다. UNPUBLISHED -> PUBLISHED (visibility 변경)
     * registrationStatus/eventStatus는 변경하지 않습니다.
     *
     * @throws InvalidEventStateTransitionException 이미 공개 상태인 경우
     */
    public void publish() {
        if (!this.visibility.canTransitionTo(EventVisibility.PUBLISHED)) {
            throw new InvalidEventStateTransitionException(this.visibility, EventVisibility.PUBLISHED);
        }
        this.visibility = EventVisibility.PUBLISHED;
    }

    /**
     * 행사를 비공개로 전환합니다. PUBLISHED -> UNPUBLISHED (visibility 변경)
     * registrationStatus가 OPEN이면 CLOSED(MANUAL_CLOSE)로 자동 마감합니다.
     * NOT_STARTED/CLOSED이면 registrationStatus를 변경하지 않습니다.
     *
     * @throws InvalidEventStateTransitionException 이미 비공개 상태인 경우
     */
    public void unpublish() {
        if (!this.visibility.canTransitionTo(EventVisibility.UNPUBLISHED)) {
            throw new InvalidEventStateTransitionException(this.visibility, EventVisibility.UNPUBLISHED);
        }
        if (this.registrationStatus == RegistrationStatus.OPEN) {
            this.registrationStatus = RegistrationStatus.CLOSED;
            this.closeReason = EventCloseReason.MANUAL_CLOSE;
        }
        this.visibility = EventVisibility.UNPUBLISHED;
    }

    // === 축 2: 등록 상태 변경 메서드 ===

    /**
     * 등록을 오픈합니다. (NOT_STARTED → OPEN)
     */
    public void openRegistration() {
        validateRegistrationTransition(RegistrationStatus.OPEN);
        this.registrationStatus = RegistrationStatus.OPEN;
        this.closeReason = null;
    }

    /**
     * 정원 초과로 등록을 마감합니다. (OPEN → CLOSED)
     */
    public void closeRegistrationByCapacity() {
        validateRegistrationTransition(RegistrationStatus.CLOSED);
        this.registrationStatus = RegistrationStatus.CLOSED;
        this.closeReason = EventCloseReason.CAPACITY_FULL;
    }

    /**
     * 기한 만료로 등록을 마감합니다. (OPEN → CLOSED)
     */
    public void closeRegistrationByDeadline() {
        validateRegistrationTransition(RegistrationStatus.CLOSED);
        this.registrationStatus = RegistrationStatus.CLOSED;
        this.closeReason = EventCloseReason.DEADLINE_PASSED;
    }

    /**
     * 운영자가 수동으로 등록을 마감합니다. (OPEN → CLOSED)
     */
    public void closeRegistrationManually() {
        validateRegistrationTransition(RegistrationStatus.CLOSED);
        this.registrationStatus = RegistrationStatus.CLOSED;
        this.closeReason = EventCloseReason.MANUAL_CLOSE;
    }

    /**
     * 등록을 수동으로 재오픈합니다. (CLOSED → OPEN)
     * EVT-INV-13의 도메인 레벨 검증 (일부)을 수행합니다.
     * 서비스에서 OPERATOR+ 권한, reason 필수 검증은 별도 수행.
     */
    public void reopenRegistration() {
        validateRegistrationTransition(RegistrationStatus.OPEN);
        this.registrationStatus = RegistrationStatus.OPEN;
        this.closeReason = null;
    }

    private void validateRegistrationTransition(RegistrationStatus target) {
        if (!this.registrationStatus.canTransitionTo(target)) {
            throw new InvalidEventStateTransitionException(this.registrationStatus, target);
        }
    }

    // === 축 3: 행사 진행 상태 변경 메서드 ===

    /**
     * 행사를 진행 중 상태로 변경합니다. (UPCOMING → ONGOING)
     */
    public void startOngoing() {
        validateEventTransition(EventStatus.ONGOING);
        this.eventStatus = EventStatus.ONGOING;
    }

    /**
     * 행사를 완료 처리합니다. (ONGOING → COMPLETED)
     * 교차 축 불변조건: registrationStatus를 CLOSED로 강제 전환 (EVT-INV-10)
     */
    public void complete() {
        validateEventTransition(EventStatus.COMPLETED);
        this.eventStatus = EventStatus.COMPLETED;
        // 교차 축 불변조건: COMPLETED → registrationStatus == CLOSED
        if (this.registrationStatus != RegistrationStatus.CLOSED) {
            this.registrationStatus = RegistrationStatus.CLOSED;
            this.closeReason = EventCloseReason.DEADLINE_PASSED;
        }
    }

    /**
     * 행사를 취소합니다. (UPCOMING/ONGOING → CANCELED)
     * 교차 축 불변조건: registrationStatus를 CLOSED로 강제 전환 (EVT-INV-11)
     */
    public void cancel() {
        validateEventTransition(EventStatus.CANCELED);
        this.eventStatus = EventStatus.CANCELED;
        // 교차 축 불변조건: CANCELED → registrationStatus == CLOSED, closeReason = MANUAL_CLOSE
        this.registrationStatus = RegistrationStatus.CLOSED;
        this.closeReason = EventCloseReason.MANUAL_CLOSE;
    }

    /**
     * 취소된 행사를 재활성화합니다. (CANCELED → UPCOMING/ONGOING)
     * 현재 시간 기반으로 Lazy Evaluation을 실행하여 올바른 상태를 복원합니다.
     *
     * @param now 현재 시간
     */
    public void reactivate(Instant now) {
        if (this.eventStatus != EventStatus.CANCELED) {
            throw new InvalidEventStateTransitionException(this.eventStatus, EventStatus.UPCOMING);
        }

        // 현재 시간 기반으로 행사 축 상태 결정
        if (now.isBefore(this.eventStartAt)) {
            this.eventStatus = EventStatus.UPCOMING;
        } else {
            this.eventStatus = EventStatus.ONGOING;
        }

        // 등록 축은 NOT_STARTED로 리셋 후 Lazy Evaluation으로 복원
        this.registrationStatus = RegistrationStatus.NOT_STARTED;
        this.closeReason = null;
        updateStatusIfNeeded(now);

        // 정원이 가득 찬 상태에서 재활성화된 경우, 등록 마감 처리 (EVT-INV-08 정합성)
        if (isFull() && this.registrationStatus == RegistrationStatus.OPEN) {
            closeRegistrationByCapacity();
        }
    }

    private void validateEventTransition(EventStatus target) {
        if (!this.eventStatus.canTransitionTo(target)) {
            throw new InvalidEventStateTransitionException(this.eventStatus, target);
        }
    }

    // === Lazy Evaluation (축 2 + 축 3 자동 전이) ===

    /**
     * 현재 시간에 따라 등록/행사 상태를 자동 갱신합니다. (Lazy Evaluation)
     * visibility(축 1)는 수동 전환만 가능하므로 Lazy Evaluation 대상이 아닙니다.
     *
     * <p>축 2 (등록):</p>
     * - NOT_STARTED → OPEN: 신청 시작일이 도래하고 eventStatus != CANCELED
     * - OPEN → CLOSED (DEADLINE_PASSED): 신청 마감일이 경과
     *
     * <p>축 3 (행사):</p>
     * - UPCOMING → ONGOING: 행사 시작일이 도래
     * - ONGOING → COMPLETED: 행사 종료일이 경과 (registrationStatus도 CLOSED 강제)
     *
     * @param now 현재 시간
     */
    public void updateStatusIfNeeded(Instant now) {
        // 축 1: 등록 상태 자동 전이
        if (this.registrationStatus == RegistrationStatus.NOT_STARTED
                && !now.isBefore(this.registrationStartAt)
                && this.eventStatus != EventStatus.CANCELED) {
            openRegistration();
        }

        if (this.registrationStatus == RegistrationStatus.OPEN
                && now.isAfter(this.registrationEndAt)) {
            closeRegistrationByDeadline();
        }

        // 축 2: 행사 상태 자동 전이 (CANCELED 상태에서는 전이하지 않음)
        if (this.eventStatus == EventStatus.UPCOMING
                && !now.isBefore(this.eventStartAt)) {
            startOngoing();
        }

        if (this.eventStatus == EventStatus.ONGOING
                && now.isAfter(this.eventEndAt)) {
            complete();
        }
    }

    // === 신청자 수 관리 ===

    /**
     * 신청자 수를 1 증가시킵니다.
     * 정원이 차면 자동으로 등록 마감 처리됩니다.
     */
    public void incrementCurrentCount() {
        this.currentCount++;
        if (isFull() && this.registrationStatus == RegistrationStatus.OPEN) {
            closeRegistrationByCapacity();
        }
    }

    /**
     * 신청자 수를 1 감소시킵니다.
     * 정원 마감 상태였다가 자리가 생기면 다시 OPEN 상태로 변경됩니다.
     *
     * @param now 현재 시간
     */
    public void decrementCurrentCount(Instant now) {
        if (this.currentCount > 0) {
            this.currentCount--;
        }
        reopenIfCapacityAvailable(now);
    }

    /**
     * 정원 마감 상태에서 자리가 생기면 다시 모집 상태로 변경합니다.
     * 단, 신청 마감일이 지났거나 행사가 취소된 경우 다시 열지 않습니다.
     *
     * @param now 현재 시간
     */
    private void reopenIfCapacityAvailable(Instant now) {
        if (this.registrationStatus == RegistrationStatus.CLOSED
                && this.closeReason == EventCloseReason.CAPACITY_FULL
                && !isFull()
                && now.isBefore(this.registrationEndAt)
                && this.eventStatus != EventStatus.CANCELED) {
            openRegistration();
        }
    }

    /**
     * 정원 마감 상태에서 자리가 생겼을 때 다시 모집 상태로 변경합니다.
     * 원자적 UPDATE 후 상태 갱신을 위해 사용합니다.
     *
     * @param now 현재 시간
     */
    public void reopenIfNeeded(Instant now) {
        reopenIfCapacityAvailable(now);
    }

    // === 조회 메서드 ===

    /**
     * 신청 가능한 상태인지 확인합니다.
     */
    public boolean isRegistrable() {
        return this.registrationStatus.isRegistrable() && !isFull();
    }

    /**
     * 정원이 찼는지 확인합니다.
     */
    public boolean isFull() {
        return this.currentCount >= this.capacity;
    }

    /**
     * 마감 사유를 Optional로 반환합니다.
     */
    public Optional<EventCloseReason> getCloseReasonOptional() {
        return Optional.ofNullable(this.closeReason);
    }

    /**
     * 남은 자리 수를 반환합니다.
     */
    public int getRemainingCapacity() {
        return Math.max(0, this.capacity - this.currentCount);
    }

    /**
     * 자동 승인(선착순) 방식인지 확인합니다.
     */
    public boolean isAutoApprove() {
        return this.registrationType == EventRegistrationType.AUTO_APPROVE;
    }

    /**
     * 수동 승인(선발제) 방식인지 확인합니다.
     */
    public boolean isManualApprove() {
        return this.registrationType == EventRegistrationType.MANUAL_APPROVE;
    }

    /**
     * 설문이 연결되어 있는지 확인합니다.
     *
     * @return 설문 연결 여부
     */
    public boolean hasSurvey() {
        return this.surveyId != null;
    }

    /**
     * 다른 행사와 시간이 겹치는지 확인합니다.
     *
     * @param otherStartAt 다른 행사 시작 시간
     * @param otherEndAt   다른 행사 종료 시간
     * @return 시간이 겹치면 true
     */
    public boolean overlaps(Instant otherStartAt, Instant otherEndAt) {
        // 겹침 조건: 내 시작 < 상대 종료 && 내 종료 > 상대 시작
        return this.eventStartAt.isBefore(otherEndAt) && this.eventEndAt.isAfter(otherStartAt);
    }

    // === 이미지 관리 ===

    /**
     * 이미지 목록을 조회합니다 (불변 리스트 반환).
     *
     * @return 이미지 목록
     */
    public List<EventImage> getImages() {
        return Collections.unmodifiableList(this.images);
    }

    /**
     * 이미지 목록을 새 URL 목록으로 교체합니다.
     * 기존 이미지를 모두 제거하고 새 이미지를 추가합니다.
     *
     * @param imageUrls 새 이미지 URL 목록
     */
    public void updateImages(List<String> imageUrls) {
        this.images.clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            this.images.add(EventImage.create(this, imageUrls.get(i), i));
        }
    }

    // === 수정 메서드 ===

    /**
     * 행사 정보를 수정합니다.
     * EVT-INV-07: 상태별 수정 정책
     * - UPCOMING: 전체 필드 수정 가능
     * - ONGOING: 부분 수정 (eventStartAt, registrationStartAt 변경 차단)
     * - COMPLETED: 수정 불가
     * - CANCELED: 수정 불가
     *
     * @throws EventNotEditableException    수정 불가능한 상태인 경우
     * @throws InvalidEventCapacityException 정원이 1 미만인 경우
     */
    public void update(String title, String description, String location,
                       Instant eventStartAt, Instant eventEndAt,
                       Instant registrationStartAt, Instant registrationEndAt,
                       Integer capacity, Long surveyId) {
        // COMPLETED 또는 CANCELED는 수정 불가
        if (this.eventStatus == EventStatus.COMPLETED || this.eventStatus == EventStatus.CANCELED) {
            throw new EventNotEditableException(this.eventStatus);
        }

        validateCapacity(capacity);

        // ONGOING: 부분 수정 — eventStartAt, registrationStartAt 변경 차단
        if (this.eventStatus == EventStatus.ONGOING) {
            if (!this.eventStartAt.equals(eventStartAt)) {
                throw new EventNotEditableException(this.eventStatus,
                        "진행 중인 행사의 시작 시각은 변경할 수 없습니다.");
            }
            if (!this.registrationStartAt.equals(registrationStartAt)) {
                throw new EventNotEditableException(this.eventStatus,
                        "진행 중인 행사의 등록 시작 시각은 변경할 수 없습니다.");
            }
            // ONGOING에서 capacity 감소 시 currentCount 이상이어야 함 (EVT-INV-01 보장)
            if (capacity < this.currentCount) {
                throw new InvalidEventCapacityException(capacity);
            }
        }

        // UPCOMING: 전체 필드 수정 가능
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.capacity = capacity;
        this.surveyId = surveyId;
    }
}
