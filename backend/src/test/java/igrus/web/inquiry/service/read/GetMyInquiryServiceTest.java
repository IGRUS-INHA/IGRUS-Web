package igrus.web.inquiry.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryAccessDeniedException;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetMyInquiryService 통합 테스트")
class GetMyInquiryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetMyInquiryService getMyInquiryService;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("내 문의 상세 조회 - 성공")
    class GetMyInquirySuccessTest {

        @Test
        @DisplayName("INQ-M-040: 내 문의 상세 조회 성공")
        void getMyInquiry_WithValidIdAndUserId_ReturnsInquiry() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createMemberInquiryService.createMemberInquiry(
                    createMemberInquiryRequest(), user.getId());

            // when
            InquiryResponse response = getMyInquiryService.getMyInquiry(createResponse.getId(), user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(createResponse.getId());
            assertThat(response.getTitle()).isEqualTo(DEFAULT_MEMBER_INQUIRY_TITLE);
        }
    }

    @Nested
    @DisplayName("내 문의 상세 조회 - 실패")
    class GetMyInquiryFailureTest {

        @Test
        @DisplayName("INQ-M-050: 다른 사용자의 문의 조회 시 예외 발생")
        void getMyInquiry_WithDifferentUserId_ThrowsException() {
            // given
            User user1 = createAndSaveUser("20231234", "test1@inha.edu", UserRole.ASSOCIATE);
            User user2 = createAndSaveUser("20235678", "test2@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createMemberInquiryService.createMemberInquiry(
                    createMemberInquiryRequest(), user1.getId());

            // when & then
            assertThatThrownBy(() -> getMyInquiryService.getMyInquiry(createResponse.getId(), user2.getId()))
                    .isInstanceOf(InquiryAccessDeniedException.class);
        }

        @Test
        @DisplayName("INQ-M-051: 존재하지 않는 문의 ID로 조회 시 예외 발생")
        void getMyInquiry_WithNonExistentId_ThrowsException() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> getMyInquiryService.getMyInquiry(99999L, user.getId()))
                    .isInstanceOf(InquiryAccessDeniedException.class);
        }
    }
}
