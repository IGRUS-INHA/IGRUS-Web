package igrus.web.inquiry.service.create;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static igrus.web.inquiry.fixture.InquiryTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreateMemberInquiryService 통합 테스트")
class CreateMemberInquiryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @Autowired
    private MemberInquiryRepository memberInquiryRepository;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("회원 문의 생성 - 성공")
    class CreateSuccessTest {

        @Test
        @DisplayName("INQ-M-001: 유효한 정보로 회원 문의 생성 성공")
        void createMemberInquiry_WithValidInfo_Success() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            CreateMemberInquiryRequest request = createMemberInquiryRequest();

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();

            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getType()).isEqualTo(InquiryType.EVENT);
                assertThat(savedInquiry.getUser().getId()).isEqualTo(user.getId());
                assertThat(savedInquiry.isMemberInquiry()).isTrue();
                return null;
            });
        }

        @Test
        @DisplayName("INQ-M-002: 첨부파일 포함 회원 문의 생성 성공")
        void createMemberInquiry_WithAttachments_Success() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            CreateMemberInquiryRequest request = createMemberInquiryRequestWithAttachments(2);

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).hasSize(2);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-M-003: 첨부파일 3개(최대) 포함 생성 성공 (INQ-INV-02 경계값)")
        void createMemberInquiry_WithMaxAttachments_Success() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            CreateMemberInquiryRequest request = createMemberInquiryRequestWithAttachments(3);

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).hasSize(3);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-M-004: 첨부파일 없이 생성 성공")
        void createMemberInquiry_WithoutAttachments_Success() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            CreateMemberInquiryRequest request = createMemberInquiryRequestWithAttachments(0);

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).isEmpty();
                return null;
            });
        }

        @ParameterizedTest
        @EnumSource(InquiryType.class)
        @DisplayName("INQ-M-005: 각 문의 유형별 생성 (5종)")
        void createMemberInquiry_EachType_Success(InquiryType type) {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
            CreateMemberInquiryRequest request = createMemberInquiryRequest(type);

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getType()).isEqualTo(type);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-M-006: 작성자 정보 자동 설정 확인 (INQ-INV-06)")
        void createMemberInquiry_AuthorInfoFromUser_Verified() {
            // given
            User user = User.create("20231234", "김철수", "user@inha.edu", "010-1234-5678",
                    "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, JoinRoute.EVERYTIME, null);
            user = userRepository.save(user);

            CreateMemberInquiryRequest request = createMemberInquiryRequest(InquiryType.JOIN, "가입 문의");

            // when
            InquiryCreateResponse response = createMemberInquiryService.createMemberInquiry(request, user.getId());

            // then
            User finalUser = user;
            transactionTemplate.execute(status -> {
                MemberInquiry savedInquiry = memberInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAuthorEmail()).isEqualTo("user@inha.edu");
                assertThat(savedInquiry.getAuthorName()).isEqualTo("김철수");
                assertThat(savedInquiry.getAuthorUserId()).isEqualTo(finalUser.getId());
                return null;
            });
        }
    }

    @Nested
    @DisplayName("회원 문의 생성 - 실패")
    class CreateFailureTest {

        @Test
        @DisplayName("INQ-M-010: 존재하지 않는 사용자 ID로 문의 생성 시 예외 발생")
        void createMemberInquiry_WithInvalidUserId_ThrowsException() {
            // given
            CreateMemberInquiryRequest request = createMemberInquiryRequest(InquiryType.ACCOUNT, "계정 문의");

            // when & then
            assertThatThrownBy(() -> createMemberInquiryService.createMemberInquiry(request, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
