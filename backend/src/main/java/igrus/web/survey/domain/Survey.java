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
 * 상태 흐름: DRAFT → PUBLISHED ⇄ CLOSED (CLOSED에서 재발행 가능)
 * 모든 상태에서 제목, 설명, 응답 권한, 마감일, 질문 구조 수정이 가능합니다. (구글폼 방식)
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

    /** 설문 상태 (DRAFT / PUBLISHED / CLOSED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "surveys_status", nullable = false, length = 20)
    private SurveyStatus status;

    /** 응답 대상 권한 (PUBLIC / ASSOCIATE / MEMBER) */
    @Enumerated(EnumType.STRING)
    @Column(name = "surveys_access_level", nullable = false, length = 20)
    private SurveyAccessLevel accessLevel;

    /** 설문 마감일 (선택, 경과 시 자동 CLOSED 전환) */
    @Column(name = "surveys_deadline")
    private Instant deadline;

    /** 설문에 포함된 질문 목록 (표시 순서 오름차순 정렬, 질문은 soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    private List<SurveyQuestion> questions = new ArrayList<>();

    // === Static factory method ===

    /**
     * 설문을 생성합니다. 초기 상태는 DRAFT입니다.
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
        survey.status = SurveyStatus.DRAFT;
        survey.accessLevel = accessLevel;
        survey.deadline = deadline;
        return survey;
    }

    // === 상태 전이 메서드 ===

    /**
     * 설문을 발행합니다. DRAFT → PUBLISHED
     *
     * @throws IllegalStateException DRAFT 상태가 아닌 경우
     */
    public void publish() {
        if (this.status != SurveyStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 발행할 수 있습니다.");
        }
        this.status = SurveyStatus.PUBLISHED;
    }

    /**
     * 설문을 마감합니다. PUBLISHED → CLOSED
     *
     * @throws IllegalStateException PUBLISHED 상태가 아닌 경우
     */
    public void close() {
        if (this.status != SurveyStatus.PUBLISHED) {
            throw new IllegalStateException("PUBLISHED 상태에서만 마감할 수 있습니다.");
        }
        this.status = SurveyStatus.CLOSED;
    }

    /**
     * 마감된 설문을 재발행합니다. CLOSED → PUBLISHED
     * 마감일이 설정되어 있다면 반드시 미래 시점이어야 합니다.
     *
     * @throws IllegalStateException CLOSED 상태가 아닌 경우
     * @throws IllegalStateException 마감일이 이미 경과한 경우
     */
    public void rePublish() {
        if (this.status != SurveyStatus.CLOSED) {
            throw new IllegalStateException("CLOSED 상태에서만 재발행할 수 있습니다.");
        }
        if (this.deadline != null && this.deadline.isBefore(Instant.now())) {
            throw new IllegalStateException("마감일이 경과한 설문은 재발행할 수 없습니다. 마감일을 먼저 수정하세요.");
        }
        this.status = SurveyStatus.PUBLISHED;
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

    // === 상태 조회 메서드 ===

    public boolean isDraft() {
        return this.status == SurveyStatus.DRAFT;
    }

    public boolean isPublished() {
        return this.status == SurveyStatus.PUBLISHED;
    }

    public boolean isClosed() {
        return this.status == SurveyStatus.CLOSED;
    }
}
