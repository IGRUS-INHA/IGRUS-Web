package igrus.web.inquiry.domain;

import igrus.web.inquiry.exception.InquiryMaxAttachmentsExceededException;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Inquiry 도메인")
class InquiryTest {

    private static final String INQUIRY_NUMBER = "INQ-2024010100001";
    private static final String TITLE = "테스트 문의";
    private static final String CONTENT = "문의 내용입니다.";
    private static final String GUEST_EMAIL = "guest@test.com";
    private static final String GUEST_NAME = "홍길동";
    private static final String PASSWORD_HASH = "hashed_password";

    @Nested
    @DisplayName("GuestInquiry.create 정적 팩토리 메서드")
    class CreateGuestInquiryTest {

        @Test
        @DisplayName("유효한 정보로 비회원 문의 생성 성공")
        void createGuestInquiry_WithValidInfo_ReturnsInquiry() {
            // when
            GuestInquiry inquiry = GuestInquiry.create(
                    INQUIRY_NUMBER, InquiryType.JOIN, TITLE, CONTENT,
                    GUEST_EMAIL, GUEST_NAME, PASSWORD_HASH
            );

            // then
            assertThat(inquiry).isNotNull();
            assertThat(inquiry.getInquiryNumber()).isEqualTo(INQUIRY_NUMBER);
            assertThat(inquiry.getType()).isEqualTo(InquiryType.JOIN);
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
            assertThat(inquiry.getTitle()).isEqualTo(TITLE);
            assertThat(inquiry.getContent()).isEqualTo(CONTENT);
            assertThat(inquiry.getEmail()).isEqualTo(GUEST_EMAIL);
            assertThat(inquiry.getName()).isEqualTo(GUEST_NAME);
            assertThat(inquiry.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        }

        @Test
        @DisplayName("비회원 문의는 isGuestInquiry가 true")
        void createGuestInquiry_IsGuestInquiry_ReturnsTrue() {
            // when
            GuestInquiry inquiry = GuestInquiry.create(
                    INQUIRY_NUMBER, InquiryType.JOIN, TITLE, CONTENT,
                    GUEST_EMAIL, GUEST_NAME, PASSWORD_HASH
            );

            // then
            assertThat(inquiry.isGuestInquiry()).isTrue();
            assertThat(inquiry.isMemberInquiry()).isFalse();
        }

        @Test
        @DisplayName("비회원 문의의 다형성 메서드 동작 확인")
        void createGuestInquiry_PolymorphicMethods_ReturnCorrectValues() {
            // when
            GuestInquiry inquiry = GuestInquiry.create(
                    INQUIRY_NUMBER, InquiryType.JOIN, TITLE, CONTENT,
                    GUEST_EMAIL, GUEST_NAME, PASSWORD_HASH
            );

            // then
            assertThat(inquiry.getAuthorName()).isEqualTo(GUEST_NAME);
            assertThat(inquiry.getAuthorEmail()).isEqualTo(GUEST_EMAIL);
            assertThat(inquiry.getAuthorUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("MemberInquiry.create 정적 팩토리 메서드")
    class CreateMemberInquiryTest {

        @Test
        @DisplayName("유효한 정보로 회원 문의 생성 성공")
        void createMemberInquiry_WithValidInfo_ReturnsInquiry() {
            // given
            User mockUser = mock(User.class);
            when(mockUser.getName()).thenReturn("홍길동");
            when(mockUser.getEmail()).thenReturn("user@test.com");
            when(mockUser.getId()).thenReturn(1L);

            // when
            MemberInquiry inquiry = MemberInquiry.create(
                    INQUIRY_NUMBER, InquiryType.EVENT, TITLE, CONTENT, mockUser
            );

            // then
            assertThat(inquiry).isNotNull();
            assertThat(inquiry.getInquiryNumber()).isEqualTo(INQUIRY_NUMBER);
            assertThat(inquiry.getType()).isEqualTo(InquiryType.EVENT);
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
            assertThat(inquiry.getTitle()).isEqualTo(TITLE);
            assertThat(inquiry.getContent()).isEqualTo(CONTENT);
            assertThat(inquiry.getUser()).isEqualTo(mockUser);
        }

        @Test
        @DisplayName("회원 문의는 isMemberInquiry가 true")
        void createMemberInquiry_IsMemberInquiry_ReturnsTrue() {
            // given
            User mockUser = mock(User.class);

            // when
            MemberInquiry inquiry = MemberInquiry.create(
                    INQUIRY_NUMBER, InquiryType.EVENT, TITLE, CONTENT, mockUser
            );

            // then
            assertThat(inquiry.isMemberInquiry()).isTrue();
            assertThat(inquiry.isGuestInquiry()).isFalse();
        }

        @Test
        @DisplayName("회원 문의의 다형성 메서드 동작 확인")
        void createMemberInquiry_PolymorphicMethods_ReturnCorrectValues() {
            // given
            User mockUser = mock(User.class);
            when(mockUser.getName()).thenReturn("홍길동");
            when(mockUser.getEmail()).thenReturn("user@test.com");
            when(mockUser.getId()).thenReturn(1L);

            // when
            MemberInquiry inquiry = MemberInquiry.create(
                    INQUIRY_NUMBER, InquiryType.EVENT, TITLE, CONTENT, mockUser
            );

            // then
            assertThat(inquiry.getAuthorName()).isEqualTo("홍길동");
            assertThat(inquiry.getAuthorEmail()).isEqualTo("user@test.com");
            assertThat(inquiry.getAuthorUserId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("상태 변경 메서드")
    class StatusChangeTest {

        @Test
        @DisplayName("changeStatus로 상태 변경 성공")
        void changeStatus_WithNewStatus_ChangesStatus() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // when
            inquiry.changeStatus(InquiryStatus.IN_PROGRESS);

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("startProcessing으로 IN_PROGRESS 상태로 변경")
        void startProcessing_ChangesStatusToInProgress() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // when
            inquiry.startProcessing();

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("complete로 COMPLETED 상태로 변경")
        void complete_ChangesStatusToCompleted() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // when
            inquiry.complete();

            // then
            assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("첨부파일 관리")
    class AttachmentManagementTest {

        @Test
        @DisplayName("첨부파일 추가 성공")
        void addAttachment_WithValidAttachment_AddsAttachment() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            InquiryAttachment attachment = InquiryAttachment.create(
                    "https://example.com/file.pdf", "file.pdf", 1024L
            );

            // when
            inquiry.addAttachment(attachment);

            // then
            assertThat(inquiry.getAttachments()).hasSize(1);
            assertThat(inquiry.getAttachments().get(0)).isEqualTo(attachment);
        }

        @Test
        @DisplayName("첨부파일 최대 3개까지 추가 가능")
        void addAttachment_UpTo3Attachments_Success() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // when
            for (int i = 0; i < 3; i++) {
                InquiryAttachment attachment = InquiryAttachment.create(
                        "https://example.com/file" + i + ".pdf", "file" + i + ".pdf", 1024L
                );
                inquiry.addAttachment(attachment);
            }

            // then
            assertThat(inquiry.getAttachments()).hasSize(3);
        }

        @Test
        @DisplayName("첨부파일 3개 초과 시 예외 발생")
        void addAttachment_MoreThan3Attachments_ThrowsException() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            for (int i = 0; i < 3; i++) {
                InquiryAttachment attachment = InquiryAttachment.create(
                        "https://example.com/file" + i + ".pdf", "file" + i + ".pdf", 1024L
                );
                inquiry.addAttachment(attachment);
            }

            InquiryAttachment extraAttachment = InquiryAttachment.create(
                    "https://example.com/extra.pdf", "extra.pdf", 1024L
            );

            // when & then
            assertThatThrownBy(() -> inquiry.addAttachment(extraAttachment))
                    .isInstanceOf(InquiryMaxAttachmentsExceededException.class);
        }

        @Test
        @DisplayName("첨부파일 목록은 수정 불가능한 리스트 반환")
        void getAttachments_ReturnsUnmodifiableList() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            InquiryAttachment attachment = InquiryAttachment.create(
                    "https://example.com/file.pdf", "file.pdf", 1024L
            );
            inquiry.addAttachment(attachment);

            // when & then
            assertThatThrownBy(() -> inquiry.getAttachments().add(attachment))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("답변 관리")
    class ReplyManagementTest {

        @Test
        @DisplayName("답변이 없을 때 hasReply는 false")
        void hasReply_WhenNoReply_ReturnsFalse() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // then
            assertThat(inquiry.hasReply()).isFalse();
        }

        @Test
        @DisplayName("답변 설정 후 hasReply는 true")
        void hasReply_WhenReplySet_ReturnsTrue() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            User mockOperator = mock(User.class);
            InquiryReply reply = InquiryReply.create("답변 내용", mockOperator);

            // when
            inquiry.setReply(reply);

            // then
            assertThat(inquiry.hasReply()).isTrue();
            assertThat(inquiry.getReply()).isEqualTo(reply);
        }
    }

    @Nested
    @DisplayName("메모 관리")
    class MemoManagementTest {

        @Test
        @DisplayName("메모 추가 성공")
        void addMemo_WithValidMemo_AddsMemo() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            User mockOperator = mock(User.class);
            InquiryMemo memo = InquiryMemo.create("메모 내용", mockOperator);

            // when
            inquiry.addMemo(memo);

            // then
            assertThat(inquiry.getMemos()).hasSize(1);
            assertThat(inquiry.getMemos().get(0)).isEqualTo(memo);
        }

        @Test
        @DisplayName("여러 개의 메모 추가 가능")
        void addMemo_MultiipleMemos_AddsAllMemos() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();
            User mockOperator = mock(User.class);

            // when
            for (int i = 0; i < 5; i++) {
                InquiryMemo memo = InquiryMemo.create("메모 " + i, mockOperator);
                inquiry.addMemo(memo);
            }

            // then
            assertThat(inquiry.getMemos()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("소유권 확인")
    class OwnershipTest {

        @Test
        @DisplayName("해당 사용자의 문의이면 isOwnedByUser는 true")
        void isOwnedByUser_WhenOwnedByUser_ReturnsTrue() {
            // given
            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(1L);
            MemberInquiry inquiry = MemberInquiry.create(
                    INQUIRY_NUMBER, InquiryType.EVENT, TITLE, CONTENT, mockUser
            );

            // then
            assertThat(inquiry.isOwnedByUser(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 사용자의 문의이면 isOwnedByUser는 false")
        void isOwnedByUser_WhenNotOwned_ReturnsFalse() {
            // given
            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(1L);
            MemberInquiry inquiry = MemberInquiry.create(
                    INQUIRY_NUMBER, InquiryType.EVENT, TITLE, CONTENT, mockUser
            );

            // then
            assertThat(inquiry.isOwnedByUser(2L)).isFalse();
        }

        @Test
        @DisplayName("비회원 문의는 isOwnedByUser가 항상 false")
        void isOwnedByUser_WhenGuestInquiry_ReturnsFalse() {
            // given
            GuestInquiry inquiry = createTestGuestInquiry();

            // then
            assertThat(inquiry.isOwnedByUser(1L)).isFalse();
        }
    }

    private GuestInquiry createTestGuestInquiry() {
        return GuestInquiry.create(
                INQUIRY_NUMBER, InquiryType.JOIN, TITLE, CONTENT,
                GUEST_EMAIL, GUEST_NAME, PASSWORD_HASH
        );
    }
}
