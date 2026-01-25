package igrus.web.inquiry.service;

import igrus.web.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class InquiryNumberGenerator {

    private static final String PREFIX = "INQ-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InquiryRepository inquiryRepository;

    public String generate() {
        String datePrefix = PREFIX + LocalDate.now().format(DATE_FORMATTER);
        long count = inquiryRepository.countByInquiryNumberPrefix(datePrefix);
        String sequence = String.format("%05d", count + 1);
        return datePrefix + sequence;
    }
}
