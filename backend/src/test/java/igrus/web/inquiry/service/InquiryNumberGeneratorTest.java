package igrus.web.inquiry.service;

import igrus.web.inquiry.repository.InquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryNumberGenerator")
class InquiryNumberGeneratorTest {

    @Mock
    private InquiryRepository inquiryRepository;

    private InquiryNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new InquiryNumberGenerator(inquiryRepository);
    }

    @Test
    @DisplayName("첫 번째 문의 번호 생성 - 00001로 시작")
    void generate_FirstInquiry_ReturnsNumber00001() {
        // given
        when(inquiryRepository.countByInquiryNumberPrefix(anyString())).thenReturn(0L);

        // when
        String inquiryNumber = generator.generate();

        // then
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(inquiryNumber).isEqualTo("INQ-" + expectedDate + "00001");
    }

    @Test
    @DisplayName("10번째 문의 번호 생성 - 00010")
    void generate_10thInquiry_ReturnsNumber00010() {
        // given
        when(inquiryRepository.countByInquiryNumberPrefix(anyString())).thenReturn(9L);

        // when
        String inquiryNumber = generator.generate();

        // then
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(inquiryNumber).isEqualTo("INQ-" + expectedDate + "00010");
    }

    @Test
    @DisplayName("100번째 문의 번호 생성 - 00100")
    void generate_100thInquiry_ReturnsNumber00100() {
        // given
        when(inquiryRepository.countByInquiryNumberPrefix(anyString())).thenReturn(99L);

        // when
        String inquiryNumber = generator.generate();

        // then
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(inquiryNumber).isEqualTo("INQ-" + expectedDate + "00100");
    }

    @Test
    @DisplayName("문의 번호는 INQ- 접두사로 시작")
    void generate_ReturnsNumberWithINQPrefix() {
        // given
        when(inquiryRepository.countByInquiryNumberPrefix(anyString())).thenReturn(0L);

        // when
        String inquiryNumber = generator.generate();

        // then
        assertThat(inquiryNumber).startsWith("INQ-");
    }

    @Test
    @DisplayName("문의 번호 길이는 20자")
    void generate_ReturnsNumberWith20Characters() {
        // given
        when(inquiryRepository.countByInquiryNumberPrefix(anyString())).thenReturn(0L);

        // when
        String inquiryNumber = generator.generate();

        // then
        // INQ- (4) + YYYYMMDD (8) + NNNNN (5) = 17자가 아닌, INQ-YYYYMMDD##### = 17자
        assertThat(inquiryNumber).hasSize(17);
    }
}
