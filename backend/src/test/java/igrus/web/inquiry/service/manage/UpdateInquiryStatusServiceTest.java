package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

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
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @Test
    @DisplayName("문의 상태 변경 성공")
    void updateInquiryStatus_WithValidStatus_Success() {
        // given
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(request);

        UpdateInquiryStatusRequest statusRequest = UpdateInquiryStatusRequest.builder()
                .status(InquiryStatus.IN_PROGRESS)
                .build();

        // when
        updateInquiryStatusService.updateInquiryStatus(createResponse.getId(), statusRequest);

        // then
        Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
        assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
    }
}
