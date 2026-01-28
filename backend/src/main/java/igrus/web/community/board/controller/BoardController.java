package igrus.web.community.board.controller;

import igrus.web.community.board.dto.response.BoardDetailResponse;
import igrus.web.community.board.dto.response.BoardListResponse;
import igrus.web.community.board.service.BoardService;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import igrus.web.common.config.SwaggerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게시판 컨트롤러.
 * 게시판 목록 조회 및 상세 조회 API를 제공합니다.
 */
@Tag(name = "Board", description = "게시판 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(
            summary = "게시판 목록 조회",
            description = "사용자 역할에 따라 접근 가능한 게시판 목록을 조회합니다. 읽기 권한이 있는 게시판만 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시판 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = BoardListResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<BoardListResponse>> getBoardList(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시판 목록 조회 요청 - userId: {}, role: {}", user.userId(), user.role());

        UserRole role = UserRole.valueOf(user.role());
        List<BoardListResponse> boards = boardService.getBoardList(role);

        return ResponseEntity.ok(boards);
    }

    @Operation(
            summary = "게시판 상세 조회",
            description = "게시판 코드로 게시판 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시판 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BoardDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시판을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{code}")
    public ResponseEntity<BoardDetailResponse> getBoardByCode(
            @Parameter(description = "게시판 코드 (notices, general, insight)", example = "general")
            @PathVariable String code,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시판 상세 조회 요청 - code: {}, userId: {}, role: {}", code, user.userId(), user.role());

        UserRole role = UserRole.valueOf(user.role());
        BoardDetailResponse board = boardService.getBoardByCode(code, role);

        return ResponseEntity.ok(board);
    }
}
