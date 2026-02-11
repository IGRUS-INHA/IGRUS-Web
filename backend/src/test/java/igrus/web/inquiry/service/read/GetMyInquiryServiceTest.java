package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryAccessDeniedException;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
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
@DisplayName("GetMyInquiryService 통합 테스트")
class GetMyInquiryServiceTest {

    @Autowired
    private GetMyInquiryService getMyInquiryService;

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

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("내 문의 상세 조회 - 성공")
    class GetMyInquirySuccessTest {

        @Test
        @DisplayName("INQ-M-040: 내 문의 상세 조회 성공")
        void getMyInquiry_WithValidIdAndUserId_ReturnsInquiry() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의")
                    .content("내용")
                    .build();
            InquiryCreateResponse createResponse = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // when
            InquiryResponse response = getMyInquiryService.getMyInquiry(createResponse.getId(), user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(createResponse.getId());
            assertThat(response.getTitle()).isEqualTo("행사 문의");
        }
    }

    @Nested
    @DisplayName("내 문의 상세 조회 - 실패")
    class GetMyInquiryFailureTest {

        @Test
        @DisplayName("INQ-M-050: 다른 사용자의 문의 조회 시 예외 발생")
        void getMyInquiry_WithDifferentUserId_ThrowsException() {
            // given
            User user1 = createAndSaveUser("20231234", "test1@inha.edu", "010-1234-5678");
            User user2 = createAndSaveUser("20235678", "test2@inha.edu", "010-5678-1234");
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의")
                    .content("내용")
                    .build();
            InquiryCreateResponse createResponse = createMemberInquiryService.createMemberInquiry(request, user1.getId());

            // when & then
            assertThatThrownBy(() -> getMyInquiryService.getMyInquiry(createResponse.getId(), user2.getId()))
                    .isInstanceOf(InquiryAccessDeniedException.class);
        }

        @Test
        @DisplayName("INQ-M-051: 존재하지 않는 문의 ID로 조회 시 예외 발생")
        void getMyInquiry_WithNonExistentId_ThrowsException() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");

            // when & then
            assertThatThrownBy(() -> getMyInquiryService.getMyInquiry(99999L, user.getId()))
                    .isInstanceOf(InquiryAccessDeniedException.class);
        }
    }
}
