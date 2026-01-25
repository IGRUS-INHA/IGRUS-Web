package igrus.web.inquiry.domain;

import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_inquiries")
@DiscriminatorValue("MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInquiry extends Inquiry {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_inquiries_user_id", nullable = false)
    private User user;

    // === 정적 팩토리 메서드 ===
    public static MemberInquiry create(String inquiryNumber, InquiryType type, String title, String content, User user) {
        return new MemberInquiry(inquiryNumber, type, title, content, user);
    }

    private MemberInquiry(String inquiryNumber, InquiryType type, String title, String content, User user) {
        super(inquiryNumber, type, title, content);
        this.user = user;
    }

    // === 추상 메서드 구현 ===
    @Override
    public String getAuthorName() {
        return user.getName();
    }

    @Override
    public String getAuthorEmail() {
        return user.getEmail();
    }

    @Override
    public Long getAuthorUserId() {
        return user.getId();
    }

    @Override
    public boolean isGuestInquiry() {
        return false;
    }

    // === 소유권 확인 ===
    @Override
    public boolean isOwnedByUser(Long userId) {
        return this.user.getId().equals(userId);
    }
}
