package igrus.web.community.board.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.dto.response.BoardDetailResponse;
import igrus.web.community.board.service.permission.CanReadBoardService;
import igrus.web.community.board.service.permission.CanWriteBoardService;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 코드로 게시판 상세 정보 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetBoardByCodeService {

    private final GetBoardEntityService getBoardEntityService;
    private final CanReadBoardService canReadBoardService;
    private final CanWriteBoardService canWriteBoardService;

    /**
     * 게시판 코드로 게시판 상세 정보를 조회합니다.
     *
     * @param code URL path variable (소문자 가능)
     * @param role 사용자 역할
     * @return 게시판 상세 정보
     */
    @Transactional(readOnly = true)
    public BoardDetailResponse getBoardByCode(String code, UserRole role) {
        log.debug("게시판 상세 조회 - code: {}, role: {}", code, role);

        Board board = getBoardEntityService.getBoardEntity(code);
        boolean canRead = canReadBoardService.canRead(board, role);
        boolean canWrite = canWriteBoardService.canWrite(board, role);

        return BoardDetailResponse.of(board, canRead, canWrite);
    }
}
