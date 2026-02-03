package igrus.web.event.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 행사 신청 엔티티.
 * 사용자가 행사에 신청한 내역을 관리합니다.
 */
@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_registrations_event_user",
                columnNames = {"event_registrations_event_id", "event_registrations_user_id"}
        ))
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "event_registrations_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "event_registrations_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "event_registrations_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "event_registrations_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventRegistration extends BaseEntity {

    /** 신청 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_registrations_id")
    private Long id;

    /** 신청한 행사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_registrations_event_id", nullable = false)
    private Event event;

    /** 신청한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_registrations_user_id", nullable = false)
    private User user;

    /** 신청 시각 */
    @Column(name = "event_registrations_registered_at", nullable = false)
    private Instant registeredAt;

    /** 신청 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_registrations_status", nullable = false, length = 20)
    private EventRegistrationStatus status = EventRegistrationStatus.WAITING;

    // === 정적 팩토리 메서드 ===

    /**
     * 행사 신청을 생성합니다.
     * 선착순(FIRST_COME): 바로 REGISTERED
     * 선발제(SELECTION): WAITING (승인 대기)
     */
    public static EventRegistration create(Event event, User user) {
        EventRegistration registration = new EventRegistration();
        registration.event = event;
        registration.user = user;
        registration.registeredAt = Instant.now();
        registration.status = event.isFirstCome()
                ? EventRegistrationStatus.REGISTERED
                : EventRegistrationStatus.WAITING;
        return registration;
    }

    // === 상태 변경 메서드 ===

    /**
     * 신청을 승인합니다. (선발제 전용)
     */
    public void approve() {
        this.status = EventRegistrationStatus.APPROVED;
    }

    /**
     * 신청을 거절합니다. (선발제 전용)
     */
    public void reject() {
        this.status = EventRegistrationStatus.REJECTED;
    }

    /**
     * 신청을 취소합니다.
     */
    public void cancel() {
        this.status = EventRegistrationStatus.CANCELED;
    }

    /**
     * 재신청합니다. (취소된 신청만 가능)
     * 선착순(FIRST_COME): REGISTERED로 복원
     * 선발제(SELECTION): WAITING으로 복원
     *
     * @throws IllegalStateException 취소 상태가 아닌 경우
     */
    public void reRegister() {
        if (!this.isCanceled()) {
            throw new IllegalStateException("취소된 신청만 재신청 가능합니다");
        }
        this.status = event.isFirstCome()
                ? EventRegistrationStatus.REGISTERED
                : EventRegistrationStatus.WAITING;
        this.registeredAt = Instant.now();
    }

    // === 조회 메서드 ===

    /**
     * 신청 완료 상태인지 확인합니다. (선착순)
     */
    public boolean isRegistered() {
        return this.status == EventRegistrationStatus.REGISTERED;
    }

    /**
     * 승인 대기 중인지 확인합니다. (선발제)
     */
    public boolean isWaiting() {
        return this.status == EventRegistrationStatus.WAITING;
    }

    /**
     * 승인되었는지 확인합니다. (선발제)
     */
    public boolean isApproved() {
        return this.status == EventRegistrationStatus.APPROVED;
    }

    /**
     * 취소되었는지 확인합니다.
     */
    public boolean isCanceled() {
        return this.status == EventRegistrationStatus.CANCELED;
    }

    /**
     * 유효한 신청인지 확인합니다. (REGISTERED 또는 APPROVED)
     */
    public boolean isActive() {
        return this.status == EventRegistrationStatus.REGISTERED
                || this.status == EventRegistrationStatus.APPROVED;
    }
}
