package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.exception.InquiryReplyNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import java.util.List;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UpdateInquiryReplyService 통합 테스트")
class UpdateInquiryReplyServiceTest {

    @Autowired
    private UpdateInquiryReplyService updateInquiryReplyService;

    @Autowired
    private CreateInquiryReplyService createInquiryReplyService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

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

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
        return userRepository.save(user);
    }

    private InquiryCreateResponse createTestInquiry() {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    @Nested
    @DisplayName("답변 수정 - 성공")
    class UpdateReplySuccessTest {

        @Test
        @DisplayName("INQ-A-050: 답변 수정 성공")
        void updateReply_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryReplyRequest replyRequest = CreateInquiryReplyRequest.builder()
                    .content("원래 답변 내용")
                    .build();
            createInquiryReplyService.createReply(createResponse.getId(), replyRequest, operator.getId());

            UpdateInquiryReplyRequest updateRequest = UpdateInquiryReplyRequest.builder()
                    .content("수정된 답변 내용")
                    .build();

            // when
            InquiryReplyResponse response = updateInquiryReplyService.updateReply(createResponse.getId(), updateRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("수정된 답변 내용");
        }
    }

    @Nested
    @DisplayName("답변 수정 - 실패")
    class UpdateReplyFailureTest {

        @Test
        @DisplayName("INQ-A-051: 답변이 없는 문의에 답변 수정 시 예외 발생")
        void updateReply_WithNoReply_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry();

            UpdateInquiryReplyRequest updateRequest = UpdateInquiryReplyRequest.builder()
                    .content("수정된 답변 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> updateInquiryReplyService.updateReply(createResponse.getId(), updateRequest, operator.getId()))
                    .isInstanceOf(InquiryReplyNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-A-052: 존재하지 않는 문의에 답변 수정 시 예외 발생")
        void updateReply_WithNonExistentInquiry_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");

            UpdateInquiryReplyRequest updateRequest = UpdateInquiryReplyRequest.builder()
                    .content("수정된 답변 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> updateInquiryReplyService.updateReply(99999L, updateRequest, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
