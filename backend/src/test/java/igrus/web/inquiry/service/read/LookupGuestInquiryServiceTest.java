package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryInvalidPasswordException;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

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
    private CreateMemberInquiryService createMemberInquiryService;

    @Autowired
    private UserRepository userRepository;

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
            entityManager.createNativeQuery("DELETE FROM event_registrations").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    private InquiryCreateResponse createTestGuestInquiry(String email, String password) {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("가입하고 싶습니다.")
                .email(email)
                .name("홍길동")
                .password(password)
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    @Nested
    @DisplayName("비회원 문의 조회 - 성공")
    class LookupSuccessTest {

        @Test
        @DisplayName("INQ-G-040: 올바른 정보로 비회원 문의 조회 성공")
        void lookupGuestInquiry_WithCorrectPassword_Success() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("guest@test.com", "password123");

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
        @DisplayName("INQ-G-041: 첨부파일 포함 문의 조회 시 첨부파일 정보 반환")
        void lookupGuestInquiry_WithAttachments_ReturnsAttachments() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .attachments(List.of(
                            AttachmentInfo.builder()
                                    .fileUrl("https://example.com/f1.pdf").fileName("f1.pdf").fileSize(1024L).build(),
                            AttachmentInfo.builder()
                                    .fileUrl("https://example.com/f2.pdf").fileName("f2.pdf").fileSize(2048L).build()
                    ))
                    .build();
            InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(request);

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(createResponse.getInquiryNumber())
                    .email("guest@test.com")
                    .password("password123")
                    .build();

            // when
            InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(lookupRequest);

            // then
            assertThat(response.getAttachments()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("비회원 문의 조회 - 실패")
    class LookupFailureTest {

        @Test
        @DisplayName("INQ-G-050: 존재하지 않는 문의번호로 조회 시 예외")
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

        @Test
        @DisplayName("INQ-G-051: 이메일 불일치로 조회 시 예외 (GAP-INQ-02, 존재 여부 미노출)")
        void lookupGuestInquiry_WithWrongEmail_ThrowsNotFoundException() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("a@test.com", "password123");

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(createResponse.getInquiryNumber())
                    .email("b@test.com")
                    .password("password123")
                    .build();

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-G-052: 비밀번호 불일치로 조회 시 예외")
        void lookupGuestInquiry_WithWrongPassword_ThrowsException() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("guest@test.com", "password123");

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
        @DisplayName("INQ-G-057: 회원 문의를 비회원 조회로 시도 시 예외")
        void lookupGuestInquiry_WithMemberInquiryNumber_ThrowsNotFoundException() {
            // given
            User user = User.create("20231234", "홍길동", "member@inha.edu", "010-1234-5678",
                    "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
            user = userRepository.save(user);

            CreateMemberInquiryRequest memberRequest = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();
            InquiryCreateResponse memberResponse = createMemberInquiryService.createMemberInquiry(memberRequest, user.getId());

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(memberResponse.getInquiryNumber())
                    .email("member@inha.edu")
                    .password("anypassword")
                    .build();

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
