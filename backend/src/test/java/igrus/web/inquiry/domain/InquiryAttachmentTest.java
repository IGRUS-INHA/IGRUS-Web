package igrus.web.inquiry.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InquiryAttachment 도메인")
class InquiryAttachmentTest {

    private static final String FILE_URL = "https://example.com/file.pdf";
    private static final String FILE_NAME = "file.pdf";
    private static final Long FILE_SIZE = 1024L;

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 첨부파일 생성 성공")
        void create_WithValidInfo_ReturnsAttachment() {
            // when
            InquiryAttachment attachment = InquiryAttachment.create(FILE_URL, FILE_NAME, FILE_SIZE);

            // then
            assertThat(attachment).isNotNull();
            assertThat(attachment.getFileUrl()).isEqualTo(FILE_URL);
            assertThat(attachment.getFileName()).isEqualTo(FILE_NAME);
            assertThat(attachment.getFileSize()).isEqualTo(FILE_SIZE);
            assertThat(attachment.getInquiry()).isNull();
        }
    }
}
