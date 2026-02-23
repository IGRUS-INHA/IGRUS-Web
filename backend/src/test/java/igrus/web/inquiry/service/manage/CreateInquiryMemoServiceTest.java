package igrus.web.inquiry.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.dto.response.InquiryMemoResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.GetInquiryDetailService;
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

@DisplayName("CreateInquiryMemoService 통합 테스트")
class CreateInquiryMemoServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CreateInquiryMemoService createInquiryMemoService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GetInquiryDetailService getInquiryDetailService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestInquiry() {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest());
    }

    @Nested
    @DisplayName("내부 메모 작성 - 성공")
    class MemoSuccessTest {

        @Test
        @DisplayName("INQ-A-060: 내부 메모 작성 성공")
        void createMemo_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryMemoRequest memoRequest = createMemoRequest();

            // when
            InquiryMemoResponse response = createInquiryMemoService.createMemo(createResponse.getId(), memoRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(DEFAULT_MEMO_CONTENT);

            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());
            assertThat(detail.getMemos()).hasSize(1);
        }

        @Test
        @DisplayName("INQ-A-061: 동일 문의에 여러 메모 작성 성공")
        void createMemo_MultipleMemos_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry();

            CreateInquiryMemoRequest memoRequest1 = createMemoRequest("첫 번째 메모");
            CreateInquiryMemoRequest memoRequest2 = createMemoRequest("두 번째 메모");
            CreateInquiryMemoRequest memoRequest3 = createMemoRequest("세 번째 메모");

            // when
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest1, operator.getId());
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest2, operator.getId());
            createInquiryMemoService.createMemo(createResponse.getId(), memoRequest3, operator.getId());

            // then
            InquiryDetailResponse detail = getInquiryDetailService.getInquiryDetail(createResponse.getId());
            assertThat(detail.getMemos()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("내부 메모 작성 - 실패")
    class MemoFailureTest {

        @Test
        @DisplayName("INQ-A-062: 존재하지 않는 문의에 메모 작성 시 예외 발생")
        void createMemo_WithNonExistentInquiry_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);

            CreateInquiryMemoRequest memoRequest = createMemoRequest();

            // when & then
            assertThatThrownBy(() -> createInquiryMemoService.createMemo(99999L, memoRequest, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
