package igrus.web.community.board.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.dto.response.BoardListResponse;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.board.service.permission.CanReadBoardService;
import igrus.web.community.board.service.permission.CanWriteBoardService;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static igrus.web.community.fixture.BoardTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * GetBoardListService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetBoardListService 단위 테스트")
class GetBoardListServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CanReadBoardService canReadBoardService;

    @Mock
    private CanWriteBoardService canWriteBoardService;

    @InjectMocks
    private GetBoardListService getBoardListService;

    private Board noticesBoard;
    private Board generalBoard;
    private Board insightBoard;

    @BeforeEach
    void setUp() {
        noticesBoard = createNoticesBoard();
        generalBoard = createGeneralBoard();
        insightBoard = createInsightBoard();
    }

    @DisplayName("정회원(MEMBER)이 게시판 목록 조회 시 3개 게시판 반환")
    @Test
    void getBoardList_WithMemberRole_ReturnsAllBoards() {
        // given
        UserRole role = UserRole.MEMBER;
        List<Board> boards = List.of(noticesBoard, generalBoard, insightBoard);

        given(boardRepository.findAllByOrderByDisplayOrderAsc()).willReturn(boards);
        given(canReadBoardService.canRead(eq(noticesBoard), eq(role))).willReturn(true);
        given(canReadBoardService.canRead(eq(generalBoard), eq(role))).willReturn(true);
        given(canReadBoardService.canRead(eq(insightBoard), eq(role))).willReturn(true);
        given(canWriteBoardService.canWrite(any(Board.class), eq(role))).willReturn(true);

        // when
        List<BoardListResponse> result = getBoardListService.getBoardList(role);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).code()).isEqualTo(BoardCode.NOTICES.name());
        assertThat(result.get(1).code()).isEqualTo(BoardCode.GENERAL.name());
        assertThat(result.get(2).code()).isEqualTo(BoardCode.INSIGHT.name());
    }

    @DisplayName("준회원(ASSOCIATE)이 게시판 목록 조회 시 모든 게시판 반환 (canRead 값으로 권한 구분)")
    @Test
    void getBoardList_WithAssociateRole_ReturnsAllBoardsWithPermissions() {
        // given
        UserRole role = UserRole.ASSOCIATE;
        List<Board> boards = List.of(noticesBoard, generalBoard, insightBoard);

        given(boardRepository.findAllByOrderByDisplayOrderAsc()).willReturn(boards);
        given(canReadBoardService.canRead(eq(noticesBoard), eq(role))).willReturn(true);
        given(canReadBoardService.canRead(eq(generalBoard), eq(role))).willReturn(false);
        given(canReadBoardService.canRead(eq(insightBoard), eq(role))).willReturn(false);
        given(canWriteBoardService.canWrite(eq(noticesBoard), eq(role))).willReturn(false);
        given(canWriteBoardService.canWrite(eq(generalBoard), eq(role))).willReturn(false);
        given(canWriteBoardService.canWrite(eq(insightBoard), eq(role))).willReturn(false);

        // when
        List<BoardListResponse> result = getBoardListService.getBoardList(role);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).code()).isEqualTo(BoardCode.NOTICES.name());
        assertThat(result.get(0).canRead()).isTrue();
        assertThat(result.get(0).canWrite()).isFalse();
        assertThat(result.get(1).code()).isEqualTo(BoardCode.GENERAL.name());
        assertThat(result.get(1).canRead()).isFalse();
        assertThat(result.get(2).code()).isEqualTo(BoardCode.INSIGHT.name());
        assertThat(result.get(2).canRead()).isFalse();
    }
}
