package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.exception.InvalidStatusTransitionException;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UpdateInquiryStatusService 통합 테스트")
class UpdateInquiryStatusServiceTest {

    @Autowired
    private UpdateInquiryStatusService updateInquiryStatusService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private InquiryRepository inquiryRepository;

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

    private InquiryCreateResponse createTestInquiry() {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    private void changeStatus(Long inquiryId, InquiryStatus status) {
        updateInquiryStatusService.updateInquiryStatus(inquiryId,
                UpdateInquiryStatusRequest.builder().status(status).build());
    }

    @Nested
    @DisplayName("유효한 상태 전이")
    class ValidTransitionTest {

        @Test
        @DisplayName("INQ-A-020: PENDING → IN_PROGRESS 상태 변경 성공")
        void updateStatus_PendingToInProgress_Success() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();

            // when
            changeStatus(createResponse.getId(), InquiryStatus.IN_PROGRESS);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("INQ-A-021: PENDING → COMPLETED 상태 변경 성공")
        void updateStatus_PendingToCompleted_Success() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();

            // when
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        }

        @Test
        @DisplayName("INQ-A-022: IN_PROGRESS → PENDING 되돌리기 성공 (GAP-INQ-05)")
        void updateStatus_InProgressToPending_Success() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();
            changeStatus(createResponse.getId(), InquiryStatus.IN_PROGRESS);

            // when
            changeStatus(createResponse.getId(), InquiryStatus.PENDING);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.PENDING);
        }

        @Test
        @DisplayName("INQ-A-023: IN_PROGRESS → COMPLETED 상태 변경 성공 (GAP-INQ-05)")
        void updateStatus_InProgressToCompleted_Success() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();
            changeStatus(createResponse.getId(), InquiryStatus.IN_PROGRESS);

            // when
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("멱등성 전이")
    class IdempotentTransitionTest {

        @Test
        @DisplayName("INQ-A-024: PENDING → PENDING 동일 상태 (멱등)")
        void updateStatus_PendingToPending_Idempotent() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();

            // when
            changeStatus(createResponse.getId(), InquiryStatus.PENDING);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.PENDING);
        }

        @Test
        @DisplayName("INQ-A-027: COMPLETED → COMPLETED 동일 상태 (멱등)")
        void updateStatus_CompletedToCompleted_Idempotent() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // when
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // then
            Inquiry updated = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("금지된 상태 전이 (GAP-INQ-01)")
    class ForbiddenTransitionTest {

        @Test
        @DisplayName("INQ-A-025: COMPLETED → PENDING 금지 (INQ-INV-07)")
        void updateStatus_CompletedToPending_ThrowsException() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> changeStatus(createResponse.getId(), InquiryStatus.PENDING))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("INQ-A-026: COMPLETED → IN_PROGRESS 금지 (INQ-INV-07)")
        void updateStatus_CompletedToInProgress_ThrowsException() {
            // given
            InquiryCreateResponse createResponse = createTestInquiry();
            changeStatus(createResponse.getId(), InquiryStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> changeStatus(createResponse.getId(), InquiryStatus.IN_PROGRESS))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionTest {

        @Test
        @DisplayName("INQ-A-029: 존재하지 않는 문의 상태 변경 시 예외")
        void updateStatus_NonExistentInquiry_ThrowsException() {
            assertThatThrownBy(() -> changeStatus(99999L, InquiryStatus.IN_PROGRESS))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
