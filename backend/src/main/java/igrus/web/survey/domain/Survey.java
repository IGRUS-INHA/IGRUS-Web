package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 설문 엔티티.
 * 운영진이 생성하는 설문의 기본 정보를 관리합니다.
 *
 * <p>2축 상태 모델:</p>
 * <ul>
 *   <li>축 1 (visibility): DRAFT → PUBLISHED (비가역)</li>
 *   <li>축 2 (responseStatus): NOT_STARTED → OPEN ⇄ CLOSED</li>
 * </ul>
 *
 * <p>휴지통:</p>
 * trashedAt != null이면 휴지통에 있는 상태.
 * 영구 삭제는 휴지통 상태에서만 가능합니다.
 *
 * <p>모든 상태에서 제목, 설명, 응답 권한, 마감일, 질문 구조 수정이 가능합니다. (구글폼 방식)</p>
 */
@Entity
@Table(name = "surveys")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "surveys_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "surveys_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "surveys_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "surveys_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "surveys_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "surveys_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "surveys_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey extends SoftDeletableEntity {

    /** 설문 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "surveys_id")
    private Long id;

    /** 설문 제목 (필수, 최대 100자) */
    @Column(name = "surveys_title", nullable = false, length = 100)
    private String title;

    /** 설문 설명 (선택, 최대 500자) */
    @Column(name = "surveys_description", length = 500)
    private String description;

    /** 축 1: 공개 상태 (DRAFT / PUBLISHED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "surveys_visibility", nullable = false, length = 20)
    private SurveyVisibility visibility;

    /** 축 2: 응답 수집 상태 (NOT_STARTED / OPEN / CLOSED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "surveys_response_status", nullable = false, length = 20)
    private SurveyResponseStatus responseStatus;

    /** 응답 대상 권한 (PUBLIC / ASSOCIATE / MEMBER) */
    @Enumerated(EnumType.STRING)
    @Column(name = "surveys_access_level", nullable = false, length = 20)
    private SurveyAccessLevel accessLevel;

    /** 설문 마감일 (선택, 경과 시 자동 CLOSED 전환) */
    @Column(name = "surveys_deadline")
    private Instant deadline;

    /** 휴지통 이동 시각 (null이면 활성 상태) */
    @Column(name = "surveys_trashed_at")
    private Instant trashedAt;

    /** 설문에 포함된 질문 목록 (표시 순서 오름차순 정렬, 질문은 soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    private List<SurveyQuestion> questions = new ArrayList<>();

    // === Static factory method ===

    /**
     * 설문을 생성합니다. 초기 상태: visibility=DRAFT, responseStatus=NOT_STARTED
     *
     * @param title       설문 제목
     * @param description 설문 설명 (null 가능)
     * @param accessLevel 응답 대상 권한
     * @param deadline    마감일 (null 가능)
     * @return 생성된 설문
     */
    public static Survey create(String title, String description, SurveyAccessLevel accessLevel, Instant deadline) {
        Survey survey = new Survey();
        survey.title = title;
        survey.description = description;
        survey.visibility = SurveyVisibility.DRAFT;
        survey.responseStatus = SurveyResponseStatus.NOT_STARTED;
        survey.accessLevel = accessLevel;
        survey.deadline = deadline;
        return survey;
    }

    // === 상태 전이 메서드 ===

    /**
     * 설문을 공개합니다. DRAFT → PUBLISHED (visibility 변경)
     * responseStatus는 CLOSED 유지 (별도로 openResponse 호출 필요)
     *
     * @throws IllegalStateException DRAFT 상태가 아닌 경우
     */
    public void publish() {
        if (!this.visibility.canTransitionTo(SurveyVisibility.PUBLISHED)) {
            throw new IllegalStateException("DRAFT 상태에서만 공개할 수 있습니다.");
        }
        this.visibility = SurveyVisibility.PUBLISHED;
    }

    /**
     * 응답 수집을 시작(또는 재개)합니다. responseStatus: NOT_STARTED/CLOSED → OPEN
     * 사전조건: visibility == PUBLISHED, 마감일 미경과
     *
     * @throws IllegalStateException PUBLISHED 상태가 아닌 경우
     * @throws IllegalStateException 이미 응답 수집 중인 경우
     * @throws IllegalStateException 마감일이 이미 경과한 경우
     */
    public void openResponse() {
        if (this.visibility != SurveyVisibility.PUBLISHED) {
            throw new IllegalStateException("공개된 설문에서만 응답을 시작할 수 있습니다.");
        }
        if (!this.responseStatus.canTransitionTo(SurveyResponseStatus.OPEN)) {
            throw new IllegalStateException("이미 응답 수집 중입니다.");
        }
        if (this.deadline != null && this.deadline.isBefore(Instant.now())) {
            throw new IllegalStateException("마감일이 경과한 설문은 응답을 시작할 수 없습니다. 마감일을 먼저 수정하세요.");
        }
        this.responseStatus = SurveyResponseStatus.OPEN;
    }

    /**
     * 응답 수집을 마감합니다. responseStatus: OPEN → CLOSED
     * NOT_STARTED에서는 호출 불가 (열지도 않았는데 마감할 수 없음)
     *
     * @throws IllegalStateException 응답 수집 중(OPEN)이 아닌 경우
     */
    public void closeResponse() {
        if (!this.responseStatus.canTransitionTo(SurveyResponseStatus.CLOSED)) {
            throw new IllegalStateException("응답 수집 중인 상태에서만 마감할 수 있습니다.");
        }
        this.responseStatus = SurveyResponseStatus.CLOSED;
    }

    /**
     * 설문을 공개하고 동시에 응답 수집을 시작합니다.
     * DRAFT 상태에서 한 번에 공개 + 응답 시작 (편의 메서드)
     *
     * @throws IllegalStateException DRAFT 상태가 아닌 경우
     * @throws IllegalStateException 마감일이 이미 경과한 경우
     */
    public void publishAndOpen() {
        publish();
        openResponse();
    }

    // === 수정 메서드 ===

    /**
     * 설문 정보를 수정합니다. 모든 상태에서 호출 가능합니다.
     *
     * @param title       설문 제목
     * @param description 설문 설명
     * @param accessLevel 응답 대상 권한
     * @param deadline    마감일
     */
    public void update(String title, String description, SurveyAccessLevel accessLevel, Instant deadline) {
        this.title = title;
        this.description = description;
        this.accessLevel = accessLevel;
        this.deadline = deadline;
    }

    // === 휴지통 메서드 ===

    /**
     * 설문을 휴지통으로 이동합니다. 모든 상태에서 호출 가능합니다.
     *
     * @throws IllegalStateException 이미 휴지통에 있는 경우
     */
    public void trash() {
        if (isTrashed()) {
            throw new IllegalStateException("이미 휴지통에 있는 설문입니다.");
        }
        this.trashedAt = Instant.now();
    }

    /**
     * 휴지통에서 복원합니다.
     *
     * @throws IllegalStateException 휴지통에 있지 않은 경우
     */
    public void restoreFromTrash() {
        if (!isTrashed()) {
            throw new IllegalStateException("휴지통에 있는 설문이 아닙니다.");
        }
        this.trashedAt = null;
    }

    /**
     * 휴지통에 있는지 확인합니다.
     *
     * @return 휴지통 여부
     */
    public boolean isTrashed() {
        return this.trashedAt != null;
    }

    /**
     * 영구 삭제합니다. 휴지통 상태에서만 가능합니다. (INV-18)
     *
     * @param deletedBy 삭제 수행자 ID
     * @throws IllegalStateException 휴지통에 있지 않은 경우
     */
    public void permanentDelete(Long deletedBy) {
        if (!isTrashed()) {
            throw new IllegalStateException("휴지통에 있는 설문만 영구 삭제할 수 있습니다.");
        }
        super.delete(deletedBy);
    }

    // === 상태 조회 메서드 ===

    public boolean isDraft() {
        return this.visibility == SurveyVisibility.DRAFT;
    }

    public boolean isPublished() {
        return this.visibility == SurveyVisibility.PUBLISHED;
    }

    public boolean isResponseOpen() {
        return this.responseStatus == SurveyResponseStatus.OPEN;
    }

    /**
     * 현재 응답을 받을 수 있는 상태인지 확인합니다.
     * 공개 + 응답 수집 중 + 휴지통 아님
     *
     * @return 응답 가능 여부
     */
    public boolean isAcceptingResponses() {
        return this.visibility == SurveyVisibility.PUBLISHED
                && this.responseStatus == SurveyResponseStatus.OPEN
                && this.trashedAt == null;
    }
}
