package igrus.web.community.board.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.dto.response.BoardListResponse;
import igrus.web.community.board.exception.BoardNotFoundException;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static igrus.web.community.fixture.BoardTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * BoardService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService 단위 테스트")
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardPermissionService boardPermissionService;

    @InjectMocks
    private BoardService boardService;

    private Board noticesBoard;
    private Board generalBoard;
    private Board insightBoard;

    @BeforeEach
    void setUp() {
        // 게시판 생성 - 픽스처 사용
        noticesBoard = createNoticesBoard();
        generalBoard = createGeneralBoard();
        insightBoard = createInsightBoard();
    }

    @Nested
    @DisplayName("게시판 목록 조회")
    class GetBoardListTest {

        @DisplayName("정회원(MEMBER)이 게시판 목록 조회 시 3개 게시판 반환")
        @Test
        void getBoardList_WithMemberRole_ReturnsAllBoards() {
            // given
            UserRole role = UserRole.MEMBER;
            List<Board> boards = List.of(noticesBoard, generalBoard, insightBoard);

            given(boardRepository.findAllByOrderByDisplayOrderAsc()).willReturn(boards);
            given(boardPermissionService.canRead(eq(noticesBoard), eq(role))).willReturn(true);
            given(boardPermissionService.canRead(eq(generalBoard), eq(role))).willReturn(true);
            given(boardPermissionService.canRead(eq(insightBoard), eq(role))).willReturn(true);
            given(boardPermissionService.canWrite(any(Board.class), eq(role))).willReturn(true);

            // when
            List<BoardListResponse> result = boardService.getBoardList(role);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).code()).isEqualTo(BoardCode.NOTICES.name());
            assertThat(result.get(1).code()).isEqualTo(BoardCode.GENERAL.name());
            assertThat(result.get(2).code()).isEqualTo(BoardCode.INSIGHT.name());
        }

        @DisplayName("준회원(ASSOCIATE)이 게시판 목록 조회 시 읽기 권한 있는 게시판만 반환 (공지사항만)")
        @Test
        void getBoardList_WithAssociateRole_ReturnsOnlyReadableBoards() {
            // given
            UserRole role = UserRole.ASSOCIATE;
            List<Board> boards = List.of(noticesBoard, generalBoard, insightBoard);

            given(boardRepository.findAllByOrderByDisplayOrderAsc()).willReturn(boards);
            given(boardPermissionService.canRead(eq(noticesBoard), eq(role))).willReturn(true);
            given(boardPermissionService.canRead(eq(generalBoard), eq(role))).willReturn(false);
            given(boardPermissionService.canRead(eq(insightBoard), eq(role))).willReturn(false);
            given(boardPermissionService.canWrite(eq(noticesBoard), eq(role))).willReturn(false);

            // when
            List<BoardListResponse> result = boardService.getBoardList(role);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo(BoardCode.NOTICES.name());
            assertThat(result.get(0).canRead()).isTrue();
            assertThat(result.get(0).canWrite()).isFalse();
        }
    }

    @Nested
    @DisplayName("게시판 코드로 조회")
    class GetBoardByCodeTest {

        @DisplayName("존재하지 않는 게시판 코드 조회 시 BoardNotFoundException 발생")
        @Test
        void getBoardEntity_WithNonExistentCode_ThrowsBoardNotFoundException() {
            // given
            String nonExistentCode = "non-existent";

            // when & then
            assertThatThrownBy(() -> boardService.getBoardEntity(nonExistentCode))
                    .isInstanceOf(BoardNotFoundException.class);
        }

        @DisplayName("존재하는 게시판 코드로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithValidCode_ReturnsBoard() {
            // given
            String validCode = "notices";
            given(boardRepository.findByCode(BoardCode.NOTICES)).willReturn(Optional.of(noticesBoard));

            // when
            Board result = boardService.getBoardEntity(validCode);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(BoardCode.NOTICES);
            assertThat(result.getName()).isNotBlank();
        }

        @DisplayName("대문자 게시판 코드로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithUppercaseCode_ReturnsBoard() {
            // given
            String validCode = "NOTICES";
            given(boardRepository.findByCode(BoardCode.NOTICES)).willReturn(Optional.of(noticesBoard));

            // when
            Board result = boardService.getBoardEntity(validCode);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(BoardCode.NOTICES);
        }

        @DisplayName("BoardCode enum으로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithBoardCodeEnum_ReturnsBoard() {
            // given
            given(boardRepository.findByCode(BoardCode.GENERAL)).willReturn(Optional.of(generalBoard));

            // when
            Board result = boardService.getBoardEntity(BoardCode.GENERAL);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(BoardCode.GENERAL);
            assertThat(result.getName()).isNotBlank();
        }

        @DisplayName("존재하지 않는 BoardCode로 조회 시 BoardNotFoundException 발생")
        @Test
        void getBoardEntity_WithNonExistentBoardCode_ThrowsBoardNotFoundException() {
            // given
            given(boardRepository.findByCode(BoardCode.NOTICES)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> boardService.getBoardEntity(BoardCode.NOTICES))
                    .isInstanceOf(BoardNotFoundException.class);
        }
    }
}
