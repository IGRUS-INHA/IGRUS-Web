package igrus.web.inquiry.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("InquiryReply 도메인")
class InquiryReplyTest {

    private static final String CONTENT = "답변 내용입니다.";

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 답변 생성 성공")
        void create_WithValidInfo_ReturnsReply() {
            // given
            User mockOperator = mock(User.class);

            // when
            InquiryReply reply = InquiryReply.create(CONTENT, mockOperator);

            // then
            assertThat(reply).isNotNull();
            assertThat(reply.getContent()).isEqualTo(CONTENT);
            assertThat(reply.getRepliedBy()).isEqualTo(mockOperator);
            assertThat(reply.getInquiry()).isNull();
        }
    }

    @Nested
    @DisplayName("updateContent 메서드")
    class UpdateContentTest {

        @Test
        @DisplayName("답변 내용 수정 성공")
        void updateContent_WithNewContent_UpdatesContent() {
            // given
            User mockOperator = mock(User.class);
            InquiryReply reply = InquiryReply.create(CONTENT, mockOperator);
            String newContent = "수정된 답변 내용입니다.";

            // when
            reply.updateContent(newContent);

            // then
            assertThat(reply.getContent()).isEqualTo(newContent);
        }
    }
}
