package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.Gender;
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
@DisplayName("GetInquiryDetailService 통합 테스트")
class GetInquiryDetailServiceTest {

    @Autowired
    private GetInquiryDetailService getInquiryDetailService;

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

    @Nested
    @DisplayName("관리자 문의 상세 조회")
    class DetailQueryTest {

        @Test
        @DisplayName("INQ-A-010: 비회원 문의 상세 조회 - isGuest=true")
        void getInquiryDetail_GuestInquiry_ReturnsGuestInfo() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("비회원 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(request);

            // when
            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());

            // then
            assertThat(detail).isNotNull();
            assertThat(detail.isGuest()).isTrue();
            assertThat(detail.getAuthorName()).isEqualTo("홍길동");
            assertThat(detail.getAuthorEmail()).isEqualTo("guest@test.com");
            assertThat(detail.getAuthorUserId()).isNull();
        }

        @Test
        @DisplayName("INQ-A-011: 회원 문의 상세 조회 - isGuest=false, authorUserId 포함")
        void getInquiryDetail_MemberInquiry_ReturnsMemberInfo() {
            // given
            User user = User.create("20231234", "김철수", "user@inha.edu", "010-1234-5678",
                    "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, List.of(), null, JoinRoute.EVERYTIME, null);
            user = userRepository.save(user);

            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();
            InquiryCreateResponse createResponse = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // when
            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());

            // then
            assertThat(detail).isNotNull();
            assertThat(detail.isGuest()).isFalse();
            assertThat(detail.getAuthorName()).isEqualTo("김철수");
            assertThat(detail.getAuthorEmail()).isEqualTo("user@inha.edu");
            assertThat(detail.getAuthorUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("INQ-A-014: 존재하지 않는 문의 상세 조회 시 예외")
        void getInquiryDetail_NonExistent_ThrowsException() {
            assertThatThrownBy(() -> getInquiryDetailService.getInquiryDetail(99999L))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
