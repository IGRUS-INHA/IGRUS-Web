package igrus.web.inquiry.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryInvalidPasswordException;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LookupGuestInquiryService 통합 테스트")
class LookupGuestInquiryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private LookupGuestInquiryService lookupGuestInquiryService;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestGuestInquiry(String email, String password) {
        return createGuestInquiryService.createGuestInquiry(createGuestInquiryRequest(email, password));
    }

    @Nested
    @DisplayName("비회원 문의 조회 - 성공")
    class LookupSuccessTest {

        @Test
        @DisplayName("INQ-G-040: 올바른 정보로 비회원 문의 조회 성공")
        void lookupGuestInquiry_WithCorrectPassword_Success() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("guest@test.com", "password123");

            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    createResponse.getInquiryNumber(), "guest@test.com", "password123");

            // when
            InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(lookupRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getInquiryNumber()).isEqualTo(createResponse.getInquiryNumber());
            assertThat(response.getTitle()).isEqualTo(DEFAULT_INQUIRY_TITLE);
        }

        @Test
        @DisplayName("INQ-G-041: 첨부파일 포함 문의 조회 시 첨부파일 정보 반환")
        void lookupGuestInquiry_WithAttachments_ReturnsAttachments() {
            // given
            InquiryCreateResponse createResponse = createGuestInquiryService.createGuestInquiry(
                    createGuestInquiryRequestWithAttachments(2));

            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    createResponse.getInquiryNumber(), DEFAULT_GUEST_EMAIL, DEFAULT_GUEST_PASSWORD);

            // when
            InquiryResponse response = lookupGuestInquiryService.lookupGuestInquiry(lookupRequest);

            // then
            assertThat(response.getAttachments()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("비회원 문의 조회 - 실패")
    class LookupFailureTest {

        @Test
        @DisplayName("INQ-G-050: 존재하지 않는 문의번호로 조회 시 예외")
        void lookupGuestInquiry_WithInvalidInquiryNumber_ThrowsException() {
            // given
            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    "INQ-INVALID", "guest@test.com", "password123");

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-G-051: 이메일 불일치로 조회 시 예외 (GAP-INQ-02, 존재 여부 미노출)")
        void lookupGuestInquiry_WithWrongEmail_ThrowsNotFoundException() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("a@test.com", "password123");

            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    createResponse.getInquiryNumber(), "b@test.com", "password123");

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }

        @Test
        @DisplayName("INQ-G-052: 비밀번호 불일치로 조회 시 예외")
        void lookupGuestInquiry_WithWrongPassword_ThrowsException() {
            // given
            InquiryCreateResponse createResponse = createTestGuestInquiry("guest@test.com", "password123");

            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    createResponse.getInquiryNumber(), "guest@test.com", "wrongpassword");

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryInvalidPasswordException.class);
        }

        @Test
        @DisplayName("INQ-G-057: 회원 문의를 비회원 조회로 시도 시 예외")
        void lookupGuestInquiry_WithMemberInquiryNumber_ThrowsNotFoundException() {
            // given
            User user = User.create("20231234", "홍길동", "member@inha.edu", "010-1234-5678",
                    "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
            user = userRepository.save(user);

            CreateMemberInquiryRequest memberRequest = createMemberInquiryRequest();
            InquiryCreateResponse memberResponse = createMemberInquiryService.createMemberInquiry(memberRequest, user.getId());

            GuestInquiryLookupRequest lookupRequest = createLookupRequest(
                    memberResponse.getInquiryNumber(), "member@inha.edu", "anypassword");

            // when & then
            assertThatThrownBy(() -> lookupGuestInquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }
}
