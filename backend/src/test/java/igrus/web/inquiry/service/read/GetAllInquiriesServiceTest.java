package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GetAllInquiriesService 통합 테스트")
class GetAllInquiriesServiceTest {

    @Autowired
    private GetAllInquiriesService getAllInquiriesService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

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
    @DisplayName("전체 문의 목록 조회 성공")
    void getAllInquiries_ReturnsAllInquiries() {
        // given
        CreateGuestInquiryRequest request1 = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest1@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        CreateGuestInquiryRequest request2 = CreateGuestInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("행사 문의")
                .content("내용")
                .email("guest2@test.com")
                .name("김철수")
                .password("password456")
                .build();

        createGuestInquiryService.createGuestInquiry(request1);
        createGuestInquiryService.createGuestInquiry(request2);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(null, null, pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("유형별 문의 목록 필터링 성공")
    void getAllInquiries_FilterByType_ReturnsFilteredInquiries() {
        // given
        CreateGuestInquiryRequest joinRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("가입 문의")
                .content("내용")
                .email("guest1@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        CreateGuestInquiryRequest eventRequest = CreateGuestInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("행사 문의")
                .content("내용")
                .email("guest2@test.com")
                .name("김철수")
                .password("password456")
                .build();

        createGuestInquiryService.createGuestInquiry(joinRequest);
        createGuestInquiryService.createGuestInquiry(eventRequest);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<InquiryListResponse> response = getAllInquiriesService.getAllInquiries(InquiryType.JOIN, null, pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getType()).isEqualTo(InquiryType.JOIN);
    }
}
