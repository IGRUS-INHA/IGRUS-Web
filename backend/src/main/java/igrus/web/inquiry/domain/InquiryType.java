package igrus.web.inquiry.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryType {
    JOIN("가입 문의"),
    EVENT("행사 문의"),
    REPORT("신고"),
    ACCOUNT("계정 문의"),
    OTHER("기타");

    private final String description;
}
