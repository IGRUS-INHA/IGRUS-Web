package igrus.web.inquiry.service.create;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.GuestInquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateGuestInquiryService 통합 테스트")
class CreateGuestInquiryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private GuestInquiryRepository guestInquiryRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("비회원 문의 생성 - 성공")
    class CreateSuccessTest {

        @Test
        @DisplayName("INQ-G-001: 유효한 정보로 비회원 문의 생성 성공")
        void createGuestInquiry_WithValidInfo_Success() {
            // given
            CreateGuestInquiryRequest request = createGuestInquiryRequest();

            // when
            InquiryCreateResponse response = createGuestInquiryService.createGuestInquiry(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getInquiryNumber()).startsWith("INQ-");

            GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
            assertThat(savedInquiry.getType()).isEqualTo(InquiryType.JOIN);
            assertThat(savedInquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
            assertThat(savedInquiry.getEmail()).isEqualTo(DEFAULT_GUEST_EMAIL);
            assertThat(savedInquiry.isGuestInquiry()).isTrue();
        }

        @Test
        @DisplayName("INQ-G-002/003: 첨부파일 포함 비회원 문의 생성 성공")
        void createGuestInquiry_WithAttachments_Success() {
            // given
            CreateGuestInquiryRequest request = createGuestInquiryRequestWithAttachments(2);

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
            CreateGuestInquiryRequest request = createGuestInquiryRequestWithAttachments(3);

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
            CreateGuestInquiryRequest request = createGuestInquiryRequestWithAttachments(0);

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
            CreateGuestInquiryRequest request = createGuestInquiryRequest(type);

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
            CreateGuestInquiryRequest request = createGuestInquiryRequest(DEFAULT_GUEST_EMAIL, plainPassword);

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
