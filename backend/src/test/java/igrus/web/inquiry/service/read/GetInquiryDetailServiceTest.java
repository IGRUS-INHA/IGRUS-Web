package igrus.web.inquiry.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetInquiryDetailService 통합 테스트")
class GetInquiryDetailServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetInquiryDetailService getInquiryDetailService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("관리자 문의 상세 조회")
    class DetailQueryTest {

        @Test
        @DisplayName("INQ-A-010: 비회원 문의 상세 조회 - isGuest=true")
        void getInquiryDetail_GuestInquiry_ReturnsGuestInfo() {
            // given
            InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest());

            // when
            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());

            // then
            assertThat(detail).isNotNull();
            assertThat(detail.isGuest()).isTrue();
            assertThat(detail.getAuthorName()).isEqualTo(DEFAULT_GUEST_NAME);
            assertThat(detail.getAuthorEmail()).isEqualTo(DEFAULT_GUEST_EMAIL);
            assertThat(detail.getAuthorUserId()).isNull();
        }

        @Test
        @DisplayName("INQ-A-011: 회원 문의 상세 조회 - isGuest=false, authorUserId 포함")
        void getInquiryDetail_MemberInquiry_ReturnsMemberInfo() {
            // given
            User user = User.create("20231234", "김철수", "user@inha.edu", "010-1234-5678",
                    "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
            user = userRepository.save(user);

            CreateMemberInquiryRequest request = createMemberInquiryRequest();
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
