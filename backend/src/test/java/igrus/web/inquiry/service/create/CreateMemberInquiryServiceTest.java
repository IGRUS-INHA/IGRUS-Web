package igrus.web.inquiry.service.create;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
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
@DisplayName("CreateMemberInquiryService 통합 테스트")
class CreateMemberInquiryServiceTest {

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @Autowired
    private MemberInquiryRepository memberInquiryRepository;

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
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("유효한 정보로 회원 문의 생성 성공")
    void createMemberInquiry_WithValidInfo_Success() {
        // given
        User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");
        CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("행사 문의")
                .content("행사에 대해 문의합니다.")
                .build();

        // when
        InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();

        transactionTemplate.execute(status -> {
            MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
            assertThat(savedInquiry.getType()).isEqualTo(InquiryType.EVENT);
            assertThat(savedInquiry.getUser().getId()).isEqualTo(user.getId());
            assertThat(savedInquiry.isMemberInquiry()).isTrue();
            return null;
        });
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 문의 생성 시 예외 발생")
    void createMemberInquiry_WithInvalidUserId_ThrowsException() {
        // given
        CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                .type(InquiryType.ACCOUNT)
                .title("계정 문의")
                .content("계정 문의 내용")
                .build();

        // when & then
        assertThatThrownBy(() -> createMemberInquiryService.createMemberInquiry(request, 999L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
