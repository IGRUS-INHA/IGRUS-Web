package igrus.web.inquiry.service.create;

import igrus.web.inquiry.domain.GuestInquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CreateGuestInquiryService 통합 테스트")
class CreateGuestInquiryServiceTest {

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GuestInquiryRepository guestInquiryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
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

    @Nested
    @DisplayName("비회원 문의 생성 - 성공")
    class CreateSuccessTest {

        @Test
        @DisplayName("INQ-G-001: 유효한 정보로 비회원 문의 생성 성공")
        void createGuestInquiry_WithValidInfo_Success() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("가입하고 싶습니다.")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getInquiryNumber()).startsWith("INQ-");

            GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
            assertThat(savedInquiry.getType()).isEqualTo(InquiryType.JOIN);
            assertThat(savedInquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
            assertThat(savedInquiry.getEmail()).isEqualTo("guest@test.com");
            assertThat(savedInquiry.isGuestInquiry()).isTrue();
        }

        @Test
        @DisplayName("INQ-G-002/003: 첨부파일 포함 비회원 문의 생성 성공")
        void createGuestInquiry_WithAttachments_Success() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.OTHER)
                    .title("기타 문의")
                    .content("기타 문의 내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .attachments(List.of(
                            AttachmentInfo.builder()
                                    .fileUrl("https://example.com/file1.pdf")
                                    .fileName("file1.pdf")
                                    .fileSize(1024L)
                                    .build(),
                            AttachmentInfo.builder()
                                    .fileUrl("https://example.com/file2.pdf")
                                    .fileName("file2.pdf")
                                    .fileSize(2048L)
                                    .build()
                    ))
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            transactionTemplate.execute(status -> {
                GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).hasSize(2);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-G-004: 첨부파일 3개(최대) 포함 문의 생성 성공 (INQ-INV-02 경계값)")
        void createGuestInquiry_WithMaxAttachments_Success() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .attachments(List.of(
                            AttachmentInfo.builder().fileUrl("https://example.com/f1.pdf").fileName("f1.pdf").fileSize(1024L).build(),
                            AttachmentInfo.builder().fileUrl("https://example.com/f2.pdf").fileName("f2.pdf").fileSize(1024L).build(),
                            AttachmentInfo.builder().fileUrl("https://example.com/f3.pdf").fileName("f3.pdf").fileSize(1024L).build()
                    ))
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            transactionTemplate.execute(status -> {
                GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).hasSize(3);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-G-005: 첨부파일 없이 문의 생성 성공")
        void createGuestInquiry_WithoutAttachments_Success() {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .attachments(List.of())
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            transactionTemplate.execute(status -> {
                GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).isEmpty();
                return null;
            });
        }

        @ParameterizedTest
        @EnumSource(InquiryType.class)
        @DisplayName("INQ-G-006: 각 문의 유형별 생성 (5종)")
        void createGuestInquiry_EachType_Success(InquiryType type) {
            // given
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(type)
                    .title("유형별 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            assertThat(response).isNotNull();
            GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
            assertThat(savedInquiry.getType()).isEqualTo(type);
        }

        @Test
        @DisplayName("INQ-G-007: 비밀번호 BCrypt 해싱 저장 확인 (INQ-INV-05)")
        void createGuestInquiry_PasswordHashed_WithBCrypt() {
            // given
            String plainPassword = "plaintext123";
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password(plainPassword)
                    .build();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
            assertThat(savedInquiry.getPasswordHash()).isNotEqualTo(plainPassword);
            assertThat(savedInquiry.getPasswordHash()).startsWith("$2");
            assertThat(passwordEncoder.matches(plainPassword, savedInquiry.getPasswordHash())).isTrue();
        }
    }
}
