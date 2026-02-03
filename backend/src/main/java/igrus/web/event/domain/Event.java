package igrus.web.event.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.event.exception.EventNotEditableException;
import igrus.web.event.exception.InvalidEventCapacityException;
import igrus.web.event.exception.InvalidEventStateTransitionException;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;

/**
 * 행사 엔티티.
 * 동아리에서 진행하는 행사 정보를 관리합니다.
 */
@Entity
@Table(name = "events")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "event_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "event_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "event_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "event_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

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


    /** 행사 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus status = EventStatus.UPCOMING;

    /** 마감 사유 (CLOSED 상태일 때만 값 존재) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_close_reason", length = 20)
    private EventCloseReason closeReason;

    /** 신청 방식 (선착순/선발제) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_registration_type", nullable = false, length = 20)
    private EventRegistrationType registrationType;

    // === 정적 팩토리 메서드 ===

    /**
     * 행사를 생성합니다.
     *
     * @throws InvalidEventCapacityException 정원이 1 미만인 경우
     */
    public static Event create(User user, String title, String description, String location,
                               Instant eventStartAt, Instant eventEndAt,
                               Instant registrationStartAt, Instant registrationEndAt,
                               Integer capacity, EventRegistrationType registrationType) {
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
        event.status = EventStatus.UPCOMING;
        event.registrationType = registrationType;
        return event;
    }

    private static void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new InvalidEventCapacityException(capacity);
        }
    }

    // === 상태 변경 메서드 ===

    /**
     * 행사를 모집 중 상태로 변경합니다.
     *
     * @throws InvalidEventStateTransitionException 전이 불가능한 상태에서 호출 시
     */
    public void open() {
        validateStateTransition(EventStatus.OPEN);
        this.status = EventStatus.OPEN;
        this.closeReason = null;
    }

    /**
     * 정원 초과로 마감합니다.
     */
    public void closeByCapacity() {
        validateStateTransition(EventStatus.CLOSED);
        this.status = EventStatus.CLOSED;
        this.closeReason = EventCloseReason.CAPACITY_FULL;
    }

    /**
     * 기한 만료로 마감합니다.
     *
     * @throws InvalidEventStateTransitionException 전이 불가능한 상태에서 호출 시
     */
    public void closeByDeadline() {
        validateStateTransition(EventStatus.CLOSED);
        this.status = EventStatus.CLOSED;
        this.closeReason = EventCloseReason.DEADLINE_PASSED;
    }

    /**
     * 운영자가 수동으로 마감합니다.
     *
     * @throws InvalidEventStateTransitionException 전이 불가능한 상태에서 호출 시
     */
    public void closeManually() {
        validateStateTransition(EventStatus.CLOSED);
        this.status = EventStatus.CLOSED;
        this.closeReason = EventCloseReason.MANUAL_CLOSE;
    }

    /**
     * 행사를 완료 처리합니다.
     *
     * @throws InvalidEventStateTransitionException 전이 불가능한 상태에서 호출 시
     */
    public void complete() {
        validateStateTransition(EventStatus.COMPLETED);
        this.status = EventStatus.COMPLETED;
    }

    /**
     * 행사를 취소합니다.
     *
     * @throws InvalidEventStateTransitionException 전이 불가능한 상태에서 호출 시
     */
    public void cancel() {
        validateStateTransition(EventStatus.CANCELED);
        this.status = EventStatus.CANCELED;
    }

    private void validateStateTransition(EventStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidEventStateTransitionException(this.status, target);
        }
    }

    /**
     * 현재 시간에 따라 상태를 자동 갱신합니다. (Lazy Evaluation)
     * - UPCOMING → OPEN: 신청 시작일이 지났을 때
     * - OPEN → CLOSED (DEADLINE_PASSED): 신청 마감일이 지났을 때
     *
     * @param now 현재 시간
     */
    public void updateStatusIfNeeded(Instant now) {
        // UPCOMING 상태에서 신청 시작일이 지났으면 OPEN으로 변경
        if (this.status == EventStatus.UPCOMING && !now.isBefore(this.registrationStartAt)) {
            this.status = EventStatus.OPEN;
        }

        // OPEN 상태에서 신청 마감일이 지났으면 CLOSED로 변경
        if (this.status == EventStatus.OPEN && now.isAfter(this.registrationEndAt)) {
            closeByDeadline();
        }
    }

    // === 신청자 수 관리 ===

    /**
     * 신청자 수를 1 증가시킵니다.
     * 정원이 차면 자동으로 마감 처리됩니다.
     */
    public void incrementCurrentCount() {
        this.currentCount++;
        if (isFull()) {
            closeByCapacity();
        }
    }

    /**
     * 신청자 수를 1 감소시킵니다.
     * 정원 마감 상태였다가 자리가 생기면 다시 OPEN 상태로 변경됩니다.
     */
    public void decrementCurrentCount() {
        if (this.currentCount > 0) {
            this.currentCount--;
        }
        reopenIfCapacityAvailable();
    }

    /**
     * 정원 마감 상태에서 자리가 생기면 다시 모집 상태로 변경합니다.
     * 단, 신청 마감일이 지났으면 다시 열지 않습니다.
     */
    private void reopenIfCapacityAvailable() {
        if (this.status == EventStatus.CLOSED
                && this.closeReason == EventCloseReason.CAPACITY_FULL
                && !isFull()
                && Instant.now().isBefore(this.registrationEndAt)) {
            open();
        }
    }

    // === 조회 메서드 ===

    /**
     * 신청 가능한 상태인지 확인합니다.
     */
    public boolean isRegistrable() {
        return this.status.isRegistrable() && !isFull();
    }

    /**
     * 정원이 찼는지 확인합니다.
     *
     * @return 정원 초과 여부
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
     *
     * @return 남은 자리 수
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

    // === 수정 메서드 ===

    /**
     * 행사 정보를 수정합니다.
     *
     * @throws EventNotEditableException 수정 불가능한 상태인 경우
     * @throws InvalidEventCapacityException 정원이 1 미만인 경우
     */
    public void update(String title, String description, String location,
                       Instant eventStartAt, Instant eventEndAt,
                       Instant registrationStartAt, Instant registrationEndAt,
                       Integer capacity) {
        if (!this.status.isEditable()) {
            throw new EventNotEditableException(this.status);
        }
        validateCapacity(capacity);

        this.title = title;
        this.description = description;
        this.location = location;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.capacity = capacity;
    }
}
