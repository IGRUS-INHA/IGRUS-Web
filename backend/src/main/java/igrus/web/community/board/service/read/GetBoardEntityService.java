package igrus.web.community.board.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.exception.BoardNotFoundException;
import igrus.web.community.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 엔티티 조회 서비스.
 * 게시판 코드(String 또는 BoardCode)로 게시판 엔티티를 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetBoardEntityService {

    private final BoardRepository boardRepository;

    /**
     * 게시판 코드(String)로 게시판 엔티티를 조회합니다.
     * URL path variable(소문자)을 받아 BoardCode로 변환합니다.
     *
     * @param code URL path variable (소문자 가능)
     * @return 게시판 엔티티
     * @throws BoardNotFoundException 게시판을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Board getBoardEntity(String code) {
        try {
            BoardCode boardCode = BoardCode.fromPathVariable(code);
            return getBoardEntity(boardCode);
        } catch (IllegalArgumentException e) {
            throw new BoardNotFoundException(code);
        }
    }

    /**
     * BoardCode enum으로 게시판 엔티티를 조회합니다.
     *
     * @param code BoardCode enum
     * @return 게시판 엔티티
     * @throws BoardNotFoundException 게시판을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Board getBoardEntity(BoardCode code) {
        return boardRepository.findByCode(code)
                .orElseThrow(() -> new BoardNotFoundException(code.name()));
    }
}
