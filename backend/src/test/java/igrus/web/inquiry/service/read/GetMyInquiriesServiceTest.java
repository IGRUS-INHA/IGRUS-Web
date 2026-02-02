package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GetMyInquiriesService 통합 테스트")
class GetMyInquiriesServiceTest {

    @Autowired
    private GetMyInquiriesService getMyInquiriesService;

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
    @DisplayName("회원의 문의 목록 조회 성공")
    void getMyInquiries_WithValidUserId_ReturnsInquiries() {
        // given
        User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");

        CreateMemberInquiryRequest request1 = CreateMemberInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("행사 문의 1")
                .content("내용 1")
                .build();
        CreateMemberInquiryRequest request2 = CreateMemberInquiryRequest.builder()
                .type(InquiryType.ACCOUNT)
                .title("계정 문의")
                .content("내용 2")
                .build();

        createMemberInquiryService.createMemberInquiry(request1, user.getId());
        createMemberInquiryService.createMemberInquiry(request2, user.getId());

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<InquiryListResponse> response = getMyInquiriesService.getMyInquiries(user.getId(), pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(2);
    }
}
