package igrus.web.inquiry.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.manage.UpdateInquiryStatusService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetAllInquiriesService 통합 테스트")
class GetAllInquiriesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetAllInquiriesService getAllInquiriesService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private UpdateInquiryStatusService updateInquiryStatusService;

    private User operator;

    @BeforeEach
    void setUp() {
        setUpBase();
        operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
    }

    private InquiryCreateResponse createTestInquiry(InquiryType type, String title, String email) {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest(type, title, email));
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
            updateInquiryStatusService.updateInquiryStatus(inquiry1.getId(),
                    updateStatusRequest(InquiryStatus.IN_PROGRESS), operator.getId());

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
            updateInquiryStatusService.updateInquiryStatus(joinInquiry.getId(),
                    updateStatusRequest(InquiryStatus.IN_PROGRESS), operator.getId());

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
