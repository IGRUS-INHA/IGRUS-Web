package igrus.web.community.board.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.board.dto.response.BoardDetailResponse;
import igrus.web.community.board.dto.response.BoardListResponse;
import igrus.web.community.board.service.read.GetBoardByCodeService;
import igrus.web.community.board.service.read.GetBoardListService;
import igrus.web.generated.api.BoardApi;
import igrus.web.generated.model.GetBoardByCode200Response;
import igrus.web.generated.model.GetBoardList200ResponseInner;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게시판 컨트롤러.
 * 게시판 목록 조회 및 상세 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BoardController implements BoardApi {

    private final GetBoardListService getBoardListService;
    private final GetBoardByCodeService getBoardByCodeService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GetBoardList200ResponseInner>> getBoardList() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시판 목록 조회 요청 - userId: {}, role: {}", user.userId(), user.role());

        UserRole role = EnumUtils.fromStringOrNull(UserRole.class, user.role());
        List<BoardListResponse> boards = getBoardListService.getBoardList(role);

        List<GetBoardList200ResponseInner> response = boards.stream()
                .map(board -> new GetBoardList200ResponseInner()
                        .code(board.code())
                        .name(board.name())
                        .description(board.description())
                        .canRead(board.canRead())
                        .canWrite(board.canWrite()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetBoardByCode200Response> getBoardByCode(String code) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시판 상세 조회 요청 - code: {}, userId: {}, role: {}", code, user.userId(), user.role());

        UserRole role = EnumUtils.fromStringOrNull(UserRole.class, user.role());
        BoardDetailResponse board = getBoardByCodeService.getBoardByCode(code, role);

        GetBoardByCode200Response response = new GetBoardByCode200Response()
                .code(board.code())
                .name(board.name())
                .description(board.description())
                .allowsAnonymous(board.allowsAnonymous())
                .allowsQuestionTag(board.allowsQuestionTag())
                .canRead(board.canRead())
                .canWrite(board.canWrite());

        return ResponseEntity.ok(response);
    }
}
