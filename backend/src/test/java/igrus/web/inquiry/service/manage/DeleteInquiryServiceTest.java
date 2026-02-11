package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.GetAllInquiriesService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DeleteInquiryService 통합 테스트")
class DeleteInquiryServiceTest {

    @Autowired
    private DeleteInquiryService deleteInquiryService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GetAllInquiriesService getAllInquiriesService;

    @Autowired
    private InquiryRepository inquiryRepository;

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

    private InquiryCreateResponse createTestInquiry(String email) {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email(email)
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    @Nested
    @DisplayName("문의 삭제 - 성공")
    class DeleteSuccessTest {

        @Test
        @DisplayName("INQ-A-070: 문의 소프트 삭제 성공")
        void deleteInquiry_WithValidId_SoftDeletes() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry("guest@test.com");

            // when
            deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId());

            // then
            assertThat(inquiryRepository.findById(createResponse.getId())).isEmpty();
            assertThat(inquiryRepository.countByIdIncludingDeleted(createResponse.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("INQ-A-072: 삭제된 문의는 목록 조회에서 제외됨")
        void deleteInquiry_ExcludedFromList() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse inquiry1 = createTestInquiry("guest1@test.com");
            createTestInquiry("guest2@test.com");

            // when
            deleteInquiryService.deleteInquiry(inquiry1.getId(), operator.getId());

            // then
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(null, null, PageRequest.of(0, 10));
            assertThat(response.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("문의 삭제 - 실패")
    class DeleteFailureTest {

        @Test
        @DisplayName("INQ-A-073: 이미 삭제된 문의 재삭제 시 예외 발생")
        void deleteInquiry_AlreadyDeleted_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            InquiryCreateResponse createResponse = createTestInquiry("guest@test.com");

            deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId());

            // when & then
            assertThatThrownBy(() -> deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-A-075: 존재하지 않는 문의 삭제 시 예외 발생")
        void deleteInquiry_NonExistent_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");

            // when & then
            assertThatThrownBy(() -> deleteInquiryService.deleteInquiry(99999L, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
