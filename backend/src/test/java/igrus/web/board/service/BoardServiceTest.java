package igrus.web.board.service;

import igrus.web.board.domain.Board;
import igrus.web.board.dto.response.BoardListResponse;
import igrus.web.board.exception.BoardNotFoundException;
import igrus.web.board.repository.BoardRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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
        noticesBoard = Board.create("notices", "공지사항", "동아리 공지사항을 확인하세요.", false, false, 1);
        generalBoard = Board.create("general", "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, false, 2);
        insightBoard = Board.create("insight", "정보공유", "유용한 정보를 공유하세요.", false, true, 3);
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
            assertThat(result.get(0).code()).isEqualTo("notices");
            assertThat(result.get(1).code()).isEqualTo("general");
            assertThat(result.get(2).code()).isEqualTo("insight");
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
            assertThat(result.get(0).code()).isEqualTo("notices");
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
            given(boardRepository.findByCode(nonExistentCode)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> boardService.getBoardEntity(nonExistentCode))
                    .isInstanceOf(BoardNotFoundException.class);
        }

        @DisplayName("존재하는 게시판 코드로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithValidCode_ReturnsBoard() {
            // given
            String validCode = "notices";
            given(boardRepository.findByCode(validCode)).willReturn(Optional.of(noticesBoard));

            // when
            Board result = boardService.getBoardEntity(validCode);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("notices");
            assertThat(result.getName()).isEqualTo("공지사항");
        }
    }
}
