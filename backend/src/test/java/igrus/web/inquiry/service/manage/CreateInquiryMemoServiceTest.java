package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.dto.response.InquiryMemoResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.GetInquiryDetailService;
import igrus.web.user.domain.Gender;
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
@DisplayName("CreateInquiryMemoService 통합 테스트")
class CreateInquiryMemoServiceTest {

    @Autowired
    private CreateInquiryMemoService createInquiryMemoService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GetInquiryDetailService getInquiryDetailService;

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
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, List.of(), null, JoinRoute.EVERYTIME, null);
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
    @DisplayName("내부 메모 작성 - 성공")
    class MemoSuccessTest {

        @Test
        @DisplayName("INQ-A-060: 내부 메모 작성 성공")
        void createMemo_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryMemoRequest memoRequest = CreateInquiryMemoRequest.builder()
                    .content("내부 메모 내용")
                    .build();

            // when
            InquiryMemoResponse response = createInquiryMemoService.createMemo(createResponse.getId(), memoRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("내부 메모 내용");

            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());
            assertThat(detail.getMemos()).hasSize(1);
        }

        @Test
        @DisplayName("INQ-A-061: 동일 문의에 여러 메모 작성 성공")
        void createMemo_MultipleMemos_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryMemoRequest memoRequest1 = CreateInquiryMemoRequest.builder()
                    .content("첫 번째 메모")
                    .build();
            CreateInquiryMemoRequest memoRequest2 = CreateInquiryMemoRequest.builder()
                    .content("두 번째 메모")
                    .build();
            CreateInquiryMemoRequest memoRequest3 = CreateInquiryMemoRequest.builder()
                    .content("세 번째 메모")
                    .build();

            // when
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest1, operator.getId());
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest2, operator.getId());
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest3, operator.getId());

            // then
            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());
            assertThat(detail.getMemos()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("내부 메모 작성 - 실패")
    class MemoFailureTest {

        @Test
        @DisplayName("INQ-A-062: 존재하지 않는 문의에 메모 작성 시 예외 발생")
        void createMemo_WithNonExistentInquiry_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");

            CreateInquiryMemoRequest memoRequest = CreateInquiryMemoRequest.builder()
                    .content("내부 메모 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> createInquiryMemoService.createMemo(99999L, memoRequest, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
