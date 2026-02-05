package igrus.web.inquiry.service.support;

import igrus.web.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Transactional
public class InquiryNumberGenerator {

    private static final String PREFIX = "INQ-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());

    private final InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public String generate() {
        String datePrefix = PREFIX + DATE_FORMATTER.format(Instant.now());
        long count = inquiryRepository.countByInquiryNumberPrefix(datePrefix);
        String sequence = String.format("%05d", count + 1);
        return datePrefix + sequence;
    }
}
