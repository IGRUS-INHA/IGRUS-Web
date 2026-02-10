package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.exception.InquiryAlreadyRepliedException;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import java.util.List;
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
@DisplayName("CreateInquiryReplyService 통합 테스트")
class CreateInquiryReplyServiceTest {

    @Autowired
    private CreateInquiryReplyService createInquiryReplyService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
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

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("답변 작성 성공")
    void createReply_WithValidRequest_Success() {
        // given
        User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
        CreateGuestInquiryRequest inquiryRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(inquiryRequest);

        CreateInquiryReplyRequest replyRequest = CreateInquiryReplyRequest.builder()
                .content("답변 내용입니다.")
                .build();

        // when
        InquiryReplyResponse response = createInquiryReplyService.createReply(createResponse.getId(), replyRequest, operator.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("답변 내용입니다.");

        transactionTemplate.execute(status -> {
            Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updatedInquiry.hasReply()).isTrue();
            assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
            return null;
        });
    }

    @Test
    @DisplayName("이미 답변이 있는 문의에 답변 작성 시 예외 발생")
    void createReply_WhenAlreadyReplied_ThrowsException() {
        // given
        User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
        CreateGuestInquiryRequest inquiryRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(inquiryRequest);

        CreateInquiryReplyRequest replyRequest = CreateInquiryReplyRequest.builder()
                .content("답변 내용입니다.")
                .build();
        createInquiryReplyService.createReply(createResponse.getId(), replyRequest, operator.getId());

        CreateInquiryReplyRequest duplicateRequest = CreateInquiryReplyRequest.builder()
                .content("중복 답변")
                .build();

        // when & then
        assertThatThrownBy(() -> createInquiryReplyService.createReply(createResponse.getId(), duplicateRequest, operator.getId()))
                .isInstanceOf(InquiryAlreadyRepliedException.class);
    }
}
