package igrus.web.event.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 행사 등록 수동 재오픈 감사 이력 엔티티.
 * 운영자가 등록을 수동으로 재오픈할 때마다 이력이 기록됩니다.
 */
@Entity
@Table(name = "event_reopen_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventReopenHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_reopen_histories_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_reopen_histories_event_id", nullable = false)
    private Event event;

    /** 재오픈 사유 */
    @Column(name = "event_reopen_histories_reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** 재오픈 수행자 (운영자) ID */
    @Column(name = "event_reopen_histories_reopened_by", nullable = false)
    private Long reopenedBy;

    /** 재오픈 시각 */
    @Column(name = "event_reopen_histories_reopened_at", nullable = false)
    private Instant reopenedAt;

    public static EventReopenHistory create(Event event, String reason, Long reopenedBy) {
        EventReopenHistory history = new EventReopenHistory();
        history.event = event;
        history.reason = reason;
        history.reopenedBy = reopenedBy;
        history.reopenedAt = Instant.now();
        return history;
    }
}
