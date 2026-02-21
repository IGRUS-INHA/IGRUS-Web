package igrus.web.inquiry.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.read.GetAllInquiriesService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DeleteInquiryService 통합 테스트")
class DeleteInquiryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private DeleteInquiryService deleteInquiryService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GetAllInquiriesService getAllInquiriesService;

    @Autowired
    private InquiryRepository inquiryRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestInquiry(String email) {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest(email));
    }

    @Nested
    @DisplayName("문의 삭제 - 성공")
    class DeleteSuccessTest {

        @Test
        @DisplayName("INQ-A-070: 문의 소프트 삭제 성공")
        void deleteInquiry_WithValidId_SoftDeletes() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry("guest@test.com");

            // when
            deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId());

            // then
            assertThat(inquiryRepository.findById(createResponse.getId())).isEmpty();
            assertThat(inquiryRepository.countByIdIncludingDeleted(createResponse.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("INQ-A-072: 삭제된 문의는 목록 조회에서 제외됨")
        void deleteInquiry_ExcludedFromList() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse inquiry1 = createTestInquiry("guest1@test.com");
            createTestInquiry("guest2@test.com");

            // when
            deleteInquiryService.deleteInquiry(inquiry1.getId(), operator.getId());

            // then
            Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(null, null, PageRequest.of(0, 10));
            assertThat(response.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("문의 삭제 - 실패")
    class DeleteFailureTest {

        @Test
        @DisplayName("INQ-A-073: 이미 삭제된 문의 재삭제 시 예외 발생")
        void deleteInquiry_AlreadyDeleted_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);
            InquiryCreateResponse createResponse = createTestInquiry("guest@test.com");

            deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId());

            // when & then
            assertThatThrownBy(() -> deleteInquiryService.deleteInquiry(createResponse.getId(), operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-A-075: 존재하지 않는 문의 삭제 시 예외 발생")
        void deleteInquiry_NonExistent_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> deleteInquiryService.deleteInquiry(99999L, operator.getId()))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
