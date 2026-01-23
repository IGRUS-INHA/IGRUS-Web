package igrus.web.inquiry.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inquiry_memos")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "inquiry_memos_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "inquiry_memos_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "inquiry_memos_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "inquiry_memos_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryMemo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_memos_id")
    private Long id;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_memos_inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "inquiry_memos_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_memos_written_by", nullable = false)
    private User writtenBy;

    // === 정적 팩토리 메서드 ===
    public static InquiryMemo create(String content, User writtenBy) {
        InquiryMemo memo = new InquiryMemo();
        memo.content = content;
        memo.writtenBy = writtenBy;
        return memo;
    }
}
