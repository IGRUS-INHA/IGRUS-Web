package igrus.web.inquiry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guest_inquiries")
@DiscriminatorValue("GUEST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestInquiry extends Inquiry {

    @Column(name = "guest_inquiries_email", nullable = false)
    private String email;

    @Column(name = "guest_inquiries_name", nullable = false, length = 50)
    private String name;

    @Column(name = "guest_inquiries_password_hash", nullable = false)
    private String passwordHash;

    // === 정적 팩토리 메서드 ===
    public static GuestInquiry create(String inquiryNumber, InquiryType type, String title, String content,
                                      String email, String name, String passwordHash) {
        return new GuestInquiry(inquiryNumber, type, title, content, email, name, passwordHash);
    }

    private GuestInquiry(String inquiryNumber, InquiryType type, String title, String content,
                         String email, String name, String passwordHash) {
        super(inquiryNumber, type, title, content);
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    // === 추상 메서드 구현 ===
    @Override
    public String getAuthorName() {
        return name;
    }

    @Override
    public String getAuthorEmail() {
        return email;
    }

    @Override
    public Long getAuthorUserId() {
        return null;
    }

    @Override
    public boolean isGuestInquiry() {
        return true;
    }
}
