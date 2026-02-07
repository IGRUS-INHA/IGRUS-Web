package igrus.web.community.board.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.dto.response.BoardListResponse;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.board.service.permission.CanReadBoardService;
import igrus.web.community.board.service.permission.CanWriteBoardService;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시판 목록 조회 서비스.
 * 사용자 역할에 따른 게시판 목록을 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetBoardListService {

    private final BoardRepository boardRepository;
    private final CanReadBoardService canReadBoardService;
    private final CanWriteBoardService canWriteBoardService;

    /**
     * 사용자 역할에 따른 게시판 목록을 조회합니다.
     * 읽기 권한이 있는 게시판만 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<BoardListResponse> getBoardList(UserRole role) {
        log.debug("게시판 목록 조회 - role: {}", role);

        List<Board> boards = boardRepository.findAllByOrderByDisplayOrderAsc();

        return boards.stream()
                .map(board -> {
                    boolean canRead = canReadBoardService.canRead(board, role);
                    boolean canWrite = canWriteBoardService.canWrite(board, role);
                    return BoardListResponse.of(board, canRead, canWrite);
                })
                .toList();
    }
}
