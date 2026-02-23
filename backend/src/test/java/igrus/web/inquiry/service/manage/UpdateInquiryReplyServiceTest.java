package igrus.web.inquiry.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.exception.InquiryReplyNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
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

@DisplayName("UpdateInquiryReplyService 통합 테스트")
class UpdateInquiryReplyServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private UpdateInquiryReplyService updateInquiryReplyService;

    @Autowired
    private CreateInquiryReplyService createInquiryReplyService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestInquiry() {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest());
    }

    @Nested
    @DisplayName("답변 수정 - 성공")
    class UpdateReplySuccessTest {

        @Test
        @DisplayName("INQ-A-050: 답변 수정 성공")
        void updateReply_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            createInquiryReplyService.createReply(createResponse.getId(), createReplyRequest("원래 답변 내용"), operator.getId());

            UpdateInquiryReplyRequest updateRequest = updateReplyRequest("수정된 답변 내용");

            // when
            InquiryReplyResponse response = updateInquiryReplyService.updateReply(createResponse.getId(), updateRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("수정된 답변 내용");
        }
    }

    @Nested
    @DisplayName("답변 수정 - 실패")
    class UpdateReplyFailureTest {

        @Test
        @DisplayName("INQ-A-051: 답변이 없는 문의에 답변 수정 시 예외 발생")
        void updateReply_WithNoReply_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            UpdateInquiryReplyRequest updateRequest = updateReplyRequest("수정된 답변 내용");

            // when & then
            assertThatThrownBy(() -> updateInquiryReplyService.updateReply(createResponse.getId(), updateRequest, operator.getId()))
                    .isInstanceOf(InquiryReplyNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-A-052: 존재하지 않는 문의에 답변 수정 시 예외 발생")
        void updateReply_WithNonExistentInquiry_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);

            UpdateInquiryReplyRequest updateRequest = updateReplyRequest("수정된 답변 내용");

            // when & then
            assertThatThrownBy(() -> updateInquiryReplyService.updateReply(99999L, updateRequest, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
