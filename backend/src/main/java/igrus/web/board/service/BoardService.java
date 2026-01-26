package igrus.web.board.service;

import igrus.web.board.domain.Board;
import igrus.web.board.domain.BoardCode;
import igrus.web.board.dto.response.BoardDetailResponse;
import igrus.web.board.dto.response.BoardListResponse;
import igrus.web.board.exception.BoardNotFoundException;
import igrus.web.board.repository.BoardRepository;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시판 서비스.
 * 게시판 목록 조회 및 상세 조회 기능을 제공합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardPermissionService boardPermissionService;

    /**
     * 사용자 역할에 따른 게시판 목록을 조회합니다.
     * 읽기 권한이 있는 게시판만 반환합니다.
     */
    public List<BoardListResponse> getBoardList(UserRole role) {
        log.debug("게시판 목록 조회 - role: {}", role);

        List<Board> boards = boardRepository.findAllByOrderByDisplayOrderAsc();

        return boards.stream()
                .map(board -> {
                    boolean canRead = boardPermissionService.canRead(board, role);
                    boolean canWrite = boardPermissionService.canWrite(board, role);
                    return BoardListResponse.of(board, canRead, canWrite);
                })
                .filter(response -> response.canRead())
                .toList();
    }

    /**
     * 게시판 코드로 게시판 상세 정보를 조회합니다.
     *
     * @param code URL path variable (소문자 가능)
     * @param role 사용자 역할
     * @return 게시판 상세 정보
     */
    public BoardDetailResponse getBoardByCode(String code, UserRole role) {
        log.debug("게시판 상세 조회 - code: {}, role: {}", code, role);

        Board board = getBoardEntity(code);
        boolean canRead = boardPermissionService.canRead(board, role);
        boolean canWrite = boardPermissionService.canWrite(board, role);

        return BoardDetailResponse.of(board, canRead, canWrite);
    }

    /**
     * 게시판 코드(String)로 게시판 엔티티를 조회합니다.
     * URL path variable(소문자)을 받아 BoardCode로 변환합니다.
     *
     * @param code URL path variable (소문자 가능)
     * @return 게시판 엔티티
     * @throws BoardNotFoundException 게시판을 찾을 수 없는 경우
     */
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
    public Board getBoardEntity(BoardCode code) {
        return boardRepository.findByCode(code)
                .orElseThrow(() -> new BoardNotFoundException(code.name()));
    }
}
