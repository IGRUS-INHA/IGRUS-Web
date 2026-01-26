package igrus.web.board.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시판 코드 enum.
 * 시스템에서 사용하는 게시판 식별자를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum BoardCode {
    NOTICES("notices"),
    GENERAL("general"),
    INSIGHT("insight");

    private final String code;

    public static BoardCode fromCode(String code) {
        for (BoardCode boardCode : values()) {
            if (boardCode.code.equals(code)) {
                return boardCode;
            }
        }
        throw new IllegalArgumentException("Unknown board code: " + code);
    }
}
