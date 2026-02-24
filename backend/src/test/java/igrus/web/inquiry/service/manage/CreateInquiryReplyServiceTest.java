package igrus.web.inquiry.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.exception.InquiryAlreadyRepliedException;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.InquiryRepository;
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

@DisplayName("CreateInquiryReplyService 통합 테스트")
class CreateInquiryReplyServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CreateInquiryReplyService createInquiryReplyService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private UpdateInquiryStatusService updateInquiryStatusService;

    @Autowired
    private InquiryRepository inquiryRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestInquiry() {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest());
    }

    @Nested
    @DisplayName("답변 작성 - 성공")
    class ReplySuccessTest {

        @Test
        @DisplayName("INQ-A-040: 답변 작성 성공 및 상태 COMPLETED 자동 변경")
        void createReply_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryReplyRequest replyRequest = createReplyRequest();

            // when
            InquiryReplyResponse response = createInquiryReplyService.createReply(createResponse.getId(), replyRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(DEFAULT_REPLY_CONTENT);

            transactionTemplate.execute(status -> {
                Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
                assertThat(updatedInquiry.hasReply()).isTrue();
                assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-A-041: IN_PROGRESS 상태 문의에 답변 시 COMPLETED 자동 변경")
        void createReply_WhenInProgress_AutoCompletesInquiry() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            // 상태를 IN_PROGRESS로 변경
            updateInquiryStatusService.updateInquiryStatus(createResponse.getId(),
                    updateStatusRequest(InquiryStatus.IN_PROGRESS));

            CreateInquiryReplyRequest replyRequest = createReplyRequest();

            // when
            createInquiryReplyService.createReply(createResponse.getId(), replyRequest, operator.getId());

            // then
            transactionTemplate.execute(status -> {
                Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
                assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
                return null;
            });
        }
    }

    @Nested
    @DisplayName("답변 작성 - 실패")
    class ReplyFailureTest {

        @Test
        @DisplayName("INQ-A-044: 이미 답변이 있는 문의에 답변 작성 시 예외 발생")
        void createReply_WhenAlreadyReplied_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            createInquiryReplyService.createReply(createResponse.getId(), createReplyRequest(), operator.getId());

            CreateInquiryReplyRequest duplicateRequest = createReplyRequest("중복 답변");

            // when & then
            assertThatThrownBy(() -> createInquiryReplyService.createReply(createResponse.getId(), duplicateRequest, operator.getId()))
                    .isInstanceOf(InquiryAlreadyRepliedException.class);
        }

        @Test
        @DisplayName("INQ-A-045: 존재하지 않는 문의에 답변 작성 시 예외 발생")
        void createReply_WithNonExistentInquiry_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);

            CreateInquiryReplyRequest replyRequest = createReplyRequest();

            // when & then
            assertThatThrownBy(() -> createInquiryReplyService.createReply(99999L, replyRequest, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
