package igrus.web.community.board.domain;

import igrus.web.community.board.exception.BoardNotFoundException;

/**
 * 게시판 코드 enum.
 * 시스템에서 사용하는 게시판 식별자를 정의합니다.
 */
public enum BoardCode {
    NOTICES,
    GENERAL,
    INSIGHT;

    /**
     * URL path variable(소문자)을 BoardCode로 변환합니다.
     *
     * @param value path variable 값 (소문자)
     * @return BoardCode enum
     * @throws BoardNotFoundException 알 수 없는 코드인 경우
     */
    public static BoardCode fromPathVariable(String value) {
        if (value == null) {
            throw new BoardNotFoundException("null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BoardNotFoundException(value);
        }
    }
}
