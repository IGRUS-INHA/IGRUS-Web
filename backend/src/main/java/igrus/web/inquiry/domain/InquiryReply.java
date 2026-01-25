package igrus.web.inquiry.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inquiry_replies")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "inquiry_replies_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "inquiry_replies_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "inquiry_replies_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "inquiry_replies_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_replies_id")
    private Long id;

    @Setter(AccessLevel.PACKAGE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_replies_inquiry_id", unique = true, nullable = false)
    private Inquiry inquiry;

    @Column(name = "inquiry_replies_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_replies_replied_by", nullable = false)
    private User repliedBy;

    // === 정적 팩토리 메서드 ===
    public static InquiryReply create(String content, User repliedBy) {
        InquiryReply reply = new InquiryReply();
        reply.content = content;
        reply.repliedBy = repliedBy;
        return reply;
    }

    // === 수정 ===
    public void updateContent(String newContent) {
        this.content = newContent;
    }
}
