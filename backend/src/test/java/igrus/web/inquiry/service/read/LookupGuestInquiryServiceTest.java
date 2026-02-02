package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryInvalidPasswordException;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LookupGuestInquiryService 통합 테스트")
class LookupGuestInquiryServiceTest {

    @Autowired
    private LookupGuestInquiryService lookupGuestInquiryService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("DELETE FROM inquiry_memos").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_replies").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_attachments").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guest_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM member_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_positions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_role_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_credentials").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_views").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_images").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @Test
    @DisplayName("올바른 비밀번호로 비회원 문의 조회 성공")
    void lookupGuestInquiry_WithCorrectPassword_Success() {
        // given
        CreateGuestInquiryRequest createRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("가입하고 싶습니다.")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(createRequest);

        GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                .inquiryNumber(createResponse.getInquiryNumber())
                .email("guest@test.com")
                .password("password123")
                .build();

        // when
        InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(lookupRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInquiryNumber()).isEqualTo(createResponse.getInquiryNumber());
        assertThat(response.getTitle()).isEqualTo("가입 문의");
    }

    @Test
    @DisplayName("틀린 비밀번호로 비회원 문의 조회 시 예외 발생")
    void lookupGuestInquiry_WithWrongPassword_ThrowsException() {
        // given
        CreateGuestInquiryRequest createRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("가입하고 싶습니다.")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(createRequest);

        GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                .inquiryNumber(createResponse.getInquiryNumber())
                .email("guest@test.com")
                .password("wrongpassword")
                .build();

        // when & then
        assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                .isInstanceOf(InquiryInvalidPasswordException.class);
    }

    @Test
    @DisplayName("존재하지 않는 문의 번호로 조회 시 예외 발생")
    void lookupGuestInquiry_WithInvalidInquiryNumber_ThrowsException() {
        // given
        GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                .inquiryNumber("INQ-INVALID")
                .email("guest@test.com")
                .password("password123")
                .build();

        // when & then
        assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                .isInstanceOf(InquiryNotFoundException.class);
    }
}
