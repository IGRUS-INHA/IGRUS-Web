package igrus.web.inquiry.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
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

@DisplayName("GetMyInquiriesService 통합 테스트")
class GetMyInquiriesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetMyInquiriesService getMyInquiriesService;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("내 문의 목록 조회 - 성공")
    class ListSuccessTest {

        @Test
        @DisplayName("INQ-M-030: 회원의 문의 목록 조회 성공")
        void getMyInquiries_WithValidUserId_ReturnsInquiries() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);

            createMemberInquiryService.createMemberInquiry(createMemberInquiryRequest(), user.getId());
            createMemberInquiryService.createMemberInquiry(
                    createMemberInquiryRequest(InquiryType.ACCOUNT, "계정 문의"), user.getId());

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getMyInquiriesService.getMyInquiries(user.getId(), pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("INQ-M-031: 문의 없는 경우 빈 페이지 반환")
        void getMyInquiries_WhenEmpty_ReturnsEmptyPage() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getMyInquiriesService.getMyInquiries(user.getId(), pageable);

            // then
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("INQ-M-032: 다른 사용자의 문의는 포함되지 않음")
        void getMyInquiries_ExcludesOtherUsersInquiries() {
            // given
            User user1 = createAndSaveUser("20231234", "user1@inha.edu", UserRole.ASSOCIATE);
            User user2 = createAndSaveUser("20235678", "user2@inha.edu", UserRole.ASSOCIATE);

            createMemberInquiryService.createMemberInquiry(
                    createMemberInquiryRequest(InquiryType.EVENT, "user1 문의"), user1.getId());
            createMemberInquiryService.createMemberInquiry(
                    createMemberInquiryRequest(InquiryType.ACCOUNT, "user2 문의"), user2.getId());

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = getMyInquiriesService.getMyInquiries(user1.getId(), pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getTitle()).isEqualTo("user1 문의");
        }
    }
}
