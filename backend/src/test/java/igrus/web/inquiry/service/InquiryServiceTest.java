package igrus.web.inquiry.service;

import igrus.web.inquiry.domain.GuestInquiry;
import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.request.*;
import igrus.web.inquiry.dto.response.*;
import igrus.web.inquiry.exception.*;
import igrus.web.inquiry.repository.GuestInquiryRepository;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InquiryService 통합 테스트")
class InquiryServiceTest {

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private MemberInquiryRepository memberInquiryRepository;

    @Autowired
    private GuestInquiryRepository guestInquiryRepository;

    @Autowired
    private UserRepository userRepository;

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
            entityManager.createNativeQuery("DELETE FROM user_role_history").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_credentials").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_images").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    private User createAndSaveUser(String studentId, String email, String phoneNumber) {
        User user = User.create(studentId, "홍길동", email, phoneNumber, "컴퓨터공학과", "테스트 동기");
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("비회원 문의 생성")
    class CreateGuestInquiryTest {

        @Test
        @DisplayName("유효한 정보로 비회원 문의 생성 성공")
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
            CreateInquiryResponse response = inquiryService.createGuestInquiry(request);

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
        @DisplayName("첨부파일 포함 비회원 문의 생성 성공")
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
            CreateInquiryResponse response = inquiryService.createGuestInquiry(request);

            // then
            transactionTemplate.execute(status -> {
                GuestInquiry savedInquiry = guestInquiryRepository.findById(response.getId()).orElseThrow();
                assertThat(savedInquiry.getAttachments()).hasSize(2);
                return null;
            });
        }
    }

    @Nested
    @DisplayName("회원 문의 생성")
    class CreateMemberInquiryTest {

        @Test
        @DisplayName("유효한 정보로 회원 문의 생성 성공")
        void createMemberInquiry_WithValidInfo_Success() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의")
                    .content("행사에 대해 문의합니다.")
                    .build();

            // when
            CreateInquiryResponse response = inquiryService.createMemberInquiry(request, user.getId());

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
        @DisplayName("존재하지 않는 사용자 ID로 문의 생성 시 예외 발생")
        void createMemberInquiry_WithInvalidUserId_ThrowsException() {
            // given
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.ACCOUNT)
                    .title("계정 문의")
                    .content("계정 문의 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> inquiryService.createMemberInquiry(request, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("비회원 문의 조회")
    class LookupGuestInquiryTest {

        @Test
        @DisplayName("올바른 비밀번호로 비회원 문의 조회 성공")
        void lookupGuestInquiry_WithCorrectPassword_Success() {
            // given
            CreateGuestInquiryRequest createRequest = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("가입하고 싶습니다.")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(createRequest);

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(createResponse.getInquiryNumber())
                    .email("guest@test.com")
                    .password("password123")
                    .build();

            // when
            InquiryResponse response = inquiryService.lookupGuestInquiry(lookupRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getInquiryNumber()).isEqualTo(createResponse.getInquiryNumber());
            assertThat(response.getTitle()).isEqualTo("가입 문의");
        }

        @Test
        @DisplayName("틀린 비밀번호로 비회원 문의 조회 시 예외 발생")
        void lookupGuestInquiry_WithWrongPassword_ThrowsException() {
            // given
            CreateGuestInquiryRequest createRequest = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("가입하고 싶습니다.")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(createRequest);

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(createResponse.getInquiryNumber())
                    .email("guest@test.com")
                    .password("wrongpassword")
                    .build();

            // when & then
            assertThatThrownBy(() -> inquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryInvalidPasswordException.class);
        }

        @Test
        @DisplayName("존재하지 않는 문의 번호로 조회 시 예외 발생")
        void lookupGuestInquiry_WithInvalidInquiryNumber_ThrowsException() {
            // given
            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber("INQ-INVALID")
                    .email("guest@test.com")
                    .password("password123")
                    .build();

            // when & then
            assertThatThrownBy(() -> inquiryService.lookupGuestInquiry(lookupRequest))
                    .isInstanceOf(InquiryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 문의 조회")
    class GetMyInquiriesTest {

        @Test
        @DisplayName("회원의 문의 목록 조회 성공")
        void getMyInquiries_WithValidUserId_ReturnsInquiries() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");

            CreateMemberInquiryRequest request1 = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의 1")
                    .content("내용 1")
                    .build();
            CreateMemberInquiryRequest request2 = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.ACCOUNT)
                    .title("계정 문의")
                    .content("내용 2")
                    .build();

            inquiryService.createMemberInquiry(request1, user.getId());
            inquiryService.createMemberInquiry(request2, user.getId());

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = inquiryService.getMyInquiries(user.getId(), pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("내 문의 상세 조회 성공")
        void getMyInquiry_WithValidIdAndUserId_ReturnsInquiry() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", "010-1234-5678");
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의")
                    .content("내용")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createMemberInquiry(request, user.getId());

            // when
            InquiryResponse response = inquiryService.getMyInquiry(createResponse.getId(), user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(createResponse.getId());
            assertThat(response.getTitle()).isEqualTo("행사 문의");
        }

        @Test
        @DisplayName("다른 사용자의 문의 조회 시 예외 발생")
        void getMyInquiry_WithDifferentUserId_ThrowsException() {
            // given
            User user1 = createAndSaveUser("20231234", "test1@inha.edu", "010-1234-5678");
            User user2 = createAndSaveUser("20235678", "test2@inha.edu", "010-5678-1234");
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("행사 문의")
                    .content("내용")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createMemberInquiry(request, user1.getId());

            // when & then
            assertThatThrownBy(() -> inquiryService.getMyInquiry(createResponse.getId(), user2.getId()))
                    .isInstanceOf(InquiryAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("관리자 기능")
    class AdminFunctionTest {

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

            inquiryService.createGuestInquiry(request1);
            inquiryService.createGuestInquiry(request2);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = inquiryService.getAllInquiries(null, null, pageable);

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

            inquiryService.createGuestInquiry(joinRequest);
            inquiryService.createGuestInquiry(eventRequest);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<InquiryListResponse> response = inquiryService.getAllInquiries(InquiryType.JOIN, null, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getType()).isEqualTo(InquiryType.JOIN);
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
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(request);

            UpdateInquiryStatusRequest statusRequest = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();

            // when
            inquiryService.updateInquiryStatus(createResponse.getId(), statusRequest);

            // then
            Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
            assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("답변 작성 성공")
        void createReply_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            CreateGuestInquiryRequest inquiryRequest = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(inquiryRequest);

            CreateInquiryReplyRequest replyRequest = CreateInquiryReplyRequest.builder()
                    .content("답변 내용입니다.")
                    .build();

            // when
            InquiryReplyResponse response = inquiryService.createReply(createResponse.getId(), replyRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("답변 내용입니다.");

            transactionTemplate.execute(status -> {
                Inquiry updatedInquiry = inquiryRepository.findById(createResponse.getId()).orElseThrow();
                assertThat(updatedInquiry.hasReply()).isTrue();
                assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
                return null;
            });
        }

        @Test
        @DisplayName("이미 답변이 있는 문의에 답변 작성 시 예외 발생")
        void createReply_WhenAlreadyReplied_ThrowsException() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            CreateGuestInquiryRequest inquiryRequest = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(inquiryRequest);

            CreateInquiryReplyRequest replyRequest = CreateInquiryReplyRequest.builder()
                    .content("답변 내용입니다.")
                    .build();
            inquiryService.createReply(createResponse.getId(), replyRequest, operator.getId());

            CreateInquiryReplyRequest duplicateRequest = CreateInquiryReplyRequest.builder()
                    .content("중복 답변")
                    .build();

            // when & then
            assertThatThrownBy(() -> inquiryService.createReply(createResponse.getId(), duplicateRequest, operator.getId()))
                    .isInstanceOf(InquiryAlreadyRepliedException.class);
        }

        @Test
        @DisplayName("내부 메모 작성 성공")
        void createMemo_WithValidRequest_Success() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            CreateGuestInquiryRequest inquiryRequest = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(inquiryRequest);

            CreateInquiryMemoRequest memoRequest = CreateInquiryMemoRequest.builder()
                    .content("내부 메모 내용")
                    .build();

            // when
            InquiryMemoResponse response = inquiryService.createMemo(createResponse.getId(), memoRequest, operator.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("내부 메모 내용");

            InquiryDetailResponse detail = inquiryService.getInquiryDetail(createResponse.getId());
            assertThat(detail.getMemos()).hasSize(1);
        }

        @Test
        @DisplayName("문의 삭제 (소프트 삭제) 성공")
        void deleteInquiry_WithValidId_SoftDeletes() {
            // given
            User operator = createAndSaveUser("20231234", "operator@inha.edu", "010-1234-5678");
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("가입 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();
            CreateInquiryResponse createResponse = inquiryService.createGuestInquiry(request);

            // when
            inquiryService.deleteInquiry(createResponse.getId(), operator.getId());

            // then
            assertThat(inquiryRepository.findById(createResponse.getId())).isEmpty();
            assertThat(inquiryRepository.countByIdIncludingDeleted(createResponse.getId())).isEqualTo(1);
        }
    }
}
