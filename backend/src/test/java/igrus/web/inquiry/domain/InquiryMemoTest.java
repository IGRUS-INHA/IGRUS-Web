package igrus.web.inquiry.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("InquiryMemo 도메인")
class InquiryMemoTest {

    private static final String CONTENT = "내부 메모 내용입니다.";

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 메모 생성 성공")
        void create_WithValidInfo_ReturnsMemo() {
            // given
            User mockOperator = mock(User.class);

            // when
            InquiryMemo memo = InquiryMemo.create(CONTENT, mockOperator);

            // then
            assertThat(memo).isNotNull();
            assertThat(memo.getContent()).isEqualTo(CONTENT);
            assertThat(memo.getWrittenBy()).isEqualTo(mockOperator);
            assertThat(memo.getInquiry()).isNull();
        }
    }
}
