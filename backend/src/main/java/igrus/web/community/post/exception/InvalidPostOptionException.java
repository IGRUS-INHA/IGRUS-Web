package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 게시글 옵션이 유효하지 않을 때 발생하는 예외.
 */
@Getter
public class InvalidPostOptionException extends CustomBaseException {

    private final String optionName;
    private final String boardCode;

    public InvalidPostOptionException(String optionName, String boardCode) {
        super(ErrorCode.POST_INVALID_ANONYMOUS_OPTION,
                "해당 게시판에서는 " + optionName + " 옵션을 사용할 수 없습니다. 게시판: " + boardCode);
        this.optionName = optionName;
        this.boardCode = boardCode;
    }
}
