package igrus.web.inquiry.fixture;

import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Inquiry 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 문의 관련 요청 DTO를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class InquiryTestFixture {

    public static final String DEFAULT_GUEST_EMAIL = "guest@test.com";
    public static final String DEFAULT_GUEST_NAME = "홍길동";
    public static final String DEFAULT_GUEST_PASSWORD = "password123";
    public static final String DEFAULT_INQUIRY_TITLE = "가입 문의";
    public static final String DEFAULT_INQUIRY_CONTENT = "내용";
    public static final InquiryType DEFAULT_GUEST_INQUIRY_TYPE = InquiryType.JOIN;
    public static final InquiryType DEFAULT_MEMBER_INQUIRY_TYPE = InquiryType.EVENT;
    public static final String DEFAULT_MEMBER_INQUIRY_TITLE = "행사 문의";
    public static final String DEFAULT_REPLY_CONTENT = "답변 내용입니다.";
    public static final String DEFAULT_MEMO_CONTENT = "내부 메모 내용";

    private InquiryTestFixture() {
    }

    // ==================== CreateGuestInquiryRequest ====================

    /**
     * 기본 비회원 문의 생성 요청을 생성합니다.
     *
     * @return 기본 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequest() {
        return createGuestInquiryRequest(DEFAULT_GUEST_EMAIL);
    }

    /**
     * 지정된 이메일로 비회원 문의 생성 요청을 생성합니다.
     *
     * @param email 이메일
     * @return 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequest(String email) {
        return CreateGuestInquiryRequest.builder()
                .type(DEFAULT_GUEST_INQUIRY_TYPE)
                .title(DEFAULT_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .email(email)
                .name(DEFAULT_GUEST_NAME)
                .password(DEFAULT_GUEST_PASSWORD)
                .build();
    }

    /**
     * 지정된 이메일과 비밀번호로 비회원 문의 생성 요청을 생성합니다.
     *
     * @param email    이메일
     * @param password 비밀번호
     * @return 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequest(String email, String password) {
        return CreateGuestInquiryRequest.builder()
                .type(DEFAULT_GUEST_INQUIRY_TYPE)
                .title(DEFAULT_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .email(email)
                .name(DEFAULT_GUEST_NAME)
                .password(password)
                .build();
    }

    /**
     * 지정된 유형, 제목, 이메일로 비회원 문의 생성 요청을 생성합니다.
     *
     * @param type  문의 유형
     * @param title 제목
     * @param email 이메일
     * @return 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequest(InquiryType type, String title, String email) {
        return CreateGuestInquiryRequest.builder()
                .type(type)
                .title(title)
                .content(DEFAULT_INQUIRY_CONTENT)
                .email(email)
                .name(DEFAULT_GUEST_NAME)
                .password(DEFAULT_GUEST_PASSWORD)
                .build();
    }

    /**
     * 지정된 유형으로 비회원 문의 생성 요청을 생성합니다.
     *
     * @param type 문의 유형
     * @return 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequest(InquiryType type) {
        return CreateGuestInquiryRequest.builder()
                .type(type)
                .title(DEFAULT_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .email(DEFAULT_GUEST_EMAIL)
                .name(DEFAULT_GUEST_NAME)
                .password(DEFAULT_GUEST_PASSWORD)
                .build();
    }

    /**
     * 첨부파일이 포함된 비회원 문의 생성 요청을 생성합니다.
     *
     * @param attachmentCount 첨부파일 개수
     * @return 첨부파일 포함 비회원 문의 생성 요청
     */
    public static CreateGuestInquiryRequest createGuestInquiryRequestWithAttachments(int attachmentCount) {
        return CreateGuestInquiryRequest.builder()
                .type(DEFAULT_GUEST_INQUIRY_TYPE)
                .title(DEFAULT_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .email(DEFAULT_GUEST_EMAIL)
                .name(DEFAULT_GUEST_NAME)
                .password(DEFAULT_GUEST_PASSWORD)
                .attachments(createAttachments(attachmentCount))
                .build();
    }

    // ==================== CreateMemberInquiryRequest ====================

    /**
     * 기본 회원 문의 생성 요청을 생성합니다.
     *
     * @return 기본 회원 문의 생성 요청
     */
    public static CreateMemberInquiryRequest createMemberInquiryRequest() {
        return CreateMemberInquiryRequest.builder()
                .type(DEFAULT_MEMBER_INQUIRY_TYPE)
                .title(DEFAULT_MEMBER_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .build();
    }

    /**
     * 지정된 유형과 제목으로 회원 문의 생성 요청을 생성합니다.
     *
     * @param type  문의 유형
     * @param title 제목
     * @return 회원 문의 생성 요청
     */
    public static CreateMemberInquiryRequest createMemberInquiryRequest(InquiryType type, String title) {
        return CreateMemberInquiryRequest.builder()
                .type(type)
                .title(title)
                .content(DEFAULT_INQUIRY_CONTENT)
                .build();
    }

    /**
     * 지정된 유형으로 회원 문의 생성 요청을 생성합니다.
     *
     * @param type 문의 유형
     * @return 회원 문의 생성 요청
     */
    public static CreateMemberInquiryRequest createMemberInquiryRequest(InquiryType type) {
        return CreateMemberInquiryRequest.builder()
                .type(type)
                .title(DEFAULT_MEMBER_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .build();
    }

    /**
     * 첨부파일이 포함된 회원 문의 생성 요청을 생성합니다.
     *
     * @param attachmentCount 첨부파일 개수
     * @return 첨부파일 포함 회원 문의 생성 요청
     */
    public static CreateMemberInquiryRequest createMemberInquiryRequestWithAttachments(int attachmentCount) {
        return CreateMemberInquiryRequest.builder()
                .type(DEFAULT_MEMBER_INQUIRY_TYPE)
                .title(DEFAULT_MEMBER_INQUIRY_TITLE)
                .content(DEFAULT_INQUIRY_CONTENT)
                .attachments(createAttachments(attachmentCount))
                .build();
    }

    // ==================== AttachmentInfo ====================

    /**
     * 지정된 개수의 첨부파일 목록을 생성합니다.
     *
     * @param count 첨부파일 개수
     * @return 첨부파일 목록
     */
    public static List<AttachmentInfo> createAttachments(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> AttachmentInfo.builder()
                        .fileUrl("https://example.com/f" + i + ".pdf")
                        .fileName("f" + i + ".pdf")
                        .fileSize(1024L * i)
                        .build())
                .toList();
    }

    // ==================== CreateInquiryReplyRequest ====================

    /**
     * 기본 답변 생성 요청을 생성합니다.
     *
     * @return 기본 답변 생성 요청
     */
    public static CreateInquiryReplyRequest createReplyRequest() {
        return CreateInquiryReplyRequest.builder()
                .content(DEFAULT_REPLY_CONTENT)
                .build();
    }

    /**
     * 지정된 내용으로 답변 생성 요청을 생성합니다.
     *
     * @param content 답변 내용
     * @return 답변 생성 요청
     */
    public static CreateInquiryReplyRequest createReplyRequest(String content) {
        return CreateInquiryReplyRequest.builder()
                .content(content)
                .build();
    }

    // ==================== UpdateInquiryReplyRequest ====================

    /**
     * 답변 수정 요청을 생성합니다.
     *
     * @param content 수정할 내용
     * @return 답변 수정 요청
     */
    public static UpdateInquiryReplyRequest updateReplyRequest(String content) {
        return UpdateInquiryReplyRequest.builder()
                .content(content)
                .build();
    }

    // ==================== CreateInquiryMemoRequest ====================

    /**
     * 기본 메모 생성 요청을 생성합니다.
     *
     * @return 기본 메모 생성 요청
     */
    public static CreateInquiryMemoRequest createMemoRequest() {
        return CreateInquiryMemoRequest.builder()
                .content(DEFAULT_MEMO_CONTENT)
                .build();
    }

    /**
     * 지정된 내용으로 메모 생성 요청을 생성합니다.
     *
     * @param content 메모 내용
     * @return 메모 생성 요청
     */
    public static CreateInquiryMemoRequest createMemoRequest(String content) {
        return CreateInquiryMemoRequest.builder()
                .content(content)
                .build();
    }

    // ==================== UpdateInquiryStatusRequest ====================

    /**
     * 상태 변경 요청을 생성합니다.
     *
     * @param status 변경할 상태
     * @return 상태 변경 요청
     */
    public static UpdateInquiryStatusRequest updateStatusRequest(InquiryStatus status) {
        return UpdateInquiryStatusRequest.builder()
                .status(status)
                .build();
    }

    // ==================== GuestInquiryLookupRequest ====================

    /**
     * 비회원 문의 조회 요청을 생성합니다.
     *
     * @param inquiryNumber 문의 번호
     * @param email         이메일
     * @param password      비밀번호
     * @return 비회원 문의 조회 요청
     */
    public static GuestInquiryLookupRequest createLookupRequest(String inquiryNumber, String email, String password) {
        return GuestInquiryLookupRequest.builder()
                .inquiryNumber(inquiryNumber)
                .email(email)
                .password(password)
                .build();
    }
}
