package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.manage.UpdateInquiryStatusService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("GetAllInquiriesService 통합 테스트")
class GetAllInquiriesServiceTest {

    @Autowired
    private GetAllInquiriesService getAllInquiriesService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private UpdateInquiryStatusService updateInquiryStatusService;

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

    private InquiryCreateResponse createTestInquiry(InquiryType type, String title, String email) {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(type)
                .title(title)
                .content("내용")
                .email(email)
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    @Nested
    @DisplayName("관리자 문의 목록 조회")
    class ListQueryTest {

        @Test
        @DisplayName("INQ-A-001: 전체 문의 목록 조회 성공")
        void getAllInquiries_ReturnsAllInquiries() {
            // given
            createTestInquiry(InquiryType.JOIN, "가입 문의", "guest1@test.com");
            createTestInquiry(InquiryType.EVENT, "행사 문의", "guest2@test.com");

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(null, null, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("INQ-A-002: 유형별 문의 목록 필터링 성공")
        void getAllInquiries_FilterByType_ReturnsFilteredInquiries() {
            // given
            createTestInquiry(InquiryType.JOIN, "가입 문의", "guest1@test.com");
            createTestInquiry(InquiryType.EVENT, "행사 문의", "guest2@test.com");

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(InquiryType.JOIN, null, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getType()).isEqualTo(InquiryType.JOIN);
        }

        @Test
        @DisplayName("INQ-A-003: 상태별 문의 목록 필터링 성공")
        void getAllInquiries_FilterByStatus_ReturnsFilteredInquiries() {
            // given
            InquiryCreateResponse inquiry1 = createTestInquiry(InquiryType.JOIN, "가입 문의", "guest1@test.com");
            createTestInquiry(InquiryType.EVENT, "행사 문의", "guest2@test.com");

            // inquiry1을 IN_PROGRESS로 변경
            UpdateInquiryStatusRequest statusRequest = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();
            updateInquiryStatusService.updateInquiryStatus(inquiry1.getId(), statusRequest);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> pendingResult = getAllInquiriesService.getAllInquiries(null, InquiryStatus.PENDING, pageable);
            Page<InquiryListResponse> inProgressResult = getAllInquiriesService.getAllInquiries(null, InquiryStatus.IN_PROGRESS, pageable);

            // then
            assertThat(pendingResult.getTotalElements()).isEqualTo(1);
            assertThat(inProgressResult.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("INQ-A-004: 유형+상태 복합 필터링 성공")
        void getAllInquiries_FilterByTypeAndStatus_ReturnsFilteredInquiries() {
            // given
            InquiryCreateResponse joinInquiry = createTestInquiry(InquiryType.JOIN, "가입 문의", "guest1@test.com");
            createTestInquiry(InquiryType.EVENT, "행사 문의", "guest2@test.com");
            createTestInquiry(InquiryType.JOIN, "가입 문의 2", "guest3@test.com");

            // joinInquiry를 IN_PROGRESS로 변경
            UpdateInquiryStatusRequest statusRequest = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();
            updateInquiryStatusService.updateInquiryStatus(joinInquiry.getId(), statusRequest);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(InquiryType.JOIN, InquiryStatus.PENDING, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getTitle()).isEqualTo("가입 문의 2");
        }

        @Test
        @DisplayName("INQ-A-005: 문의가 없는 경우 빈 목록 반환")
        void getAllInquiries_WhenEmpty_ReturnsEmptyPage() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(null, null, pageable);

            // then
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getContent()).isEmpty();
        }
    }
}
