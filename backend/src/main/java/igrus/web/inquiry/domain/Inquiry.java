package igrus.web.inquiry.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.inquiry.exception.InquiryMaxAttachmentsExceededException;
import igrus.web.inquiry.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "inquiries")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "inquiries_author_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@SQLRestriction("inquiries_deleted = false")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "inquiries_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "inquiries_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "inquiries_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "inquiries_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "inquiries_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "inquiries_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "inquiries_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Inquiry extends SoftDeletableEntity {

    protected static final int MAX_ATTACHMENTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiries_id")
    private Long id;

    @Column(name = "inquiries_inquiry_number", unique = true, nullable = false, length = 20)
    private String inquiryNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiries_type", nullable = false, length = 20)
    private InquiryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiries_status", nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "inquiries_title", nullable = false, length = 100)
    private String title;

    @Column(name = "inquiries_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryAttachment> attachments = new ArrayList<>();

    @OneToOne(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private InquiryReply reply;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InquiryMemo> memos = new ArrayList<>();

    // === 생성자 (하위 클래스에서 사용) ===
    protected Inquiry(String inquiryNumber, InquiryType type, String title, String content) {
        this.inquiryNumber = inquiryNumber;
        this.type = type;
        this.status = InquiryStatus.PENDING;
        this.title = title;
        this.content = content;
    }

    // === 추상 메서드: 다형성으로 분기 로직 제거 ===
    public abstract String getAuthorName();

    public abstract String getAuthorEmail();

    public abstract Long getAuthorUserId();

    public abstract boolean isGuestInquiry();

    // === 상태 변경 ===
    public void changeStatus(InquiryStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(this.status, newStatus);
        }
        this.status = newStatus;
    }

    public void startProcessing() {
        this.status = InquiryStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = InquiryStatus.COMPLETED;
    }

    // === 첨부파일 관리 ===
    public void addAttachment(InquiryAttachment attachment) {
        if (this.attachments.size() >= MAX_ATTACHMENTS) {
            throw new InquiryMaxAttachmentsExceededException(this.attachments.size() + 1);
        }
        this.attachments.add(attachment);
        attachment.setInquiry(this);
    }

    public List<InquiryAttachment> getAttachments() {
        return Collections.unmodifiableList(this.attachments);
    }

    // === 답변 관리 ===
    public boolean hasReply() {
        return this.reply != null;
    }

    public void setReply(InquiryReply reply) {
        this.reply = reply;
        reply.setInquiry(this);
    }

    // === 메모 관리 ===
    public void addMemo(InquiryMemo memo) {
        this.memos.add(memo);
        memo.setInquiry(this);
    }

    public List<InquiryMemo> getMemos() {
        return Collections.unmodifiableList(this.memos);
    }

    // === 소유권 확인 (하위 클래스에서 오버라이드 가능) ===
    public boolean isOwnedByUser(Long userId) {
        return false;
    }

    public boolean isMemberInquiry() {
        return !isGuestInquiry();
    }
}
