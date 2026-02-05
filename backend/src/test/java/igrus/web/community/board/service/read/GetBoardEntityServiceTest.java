package igrus.web.community.board.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.exception.BoardNotFoundException;
import igrus.web.community.board.repository.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static igrus.web.community.fixture.BoardTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * GetBoardEntityService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetBoardEntityService 단위 테스트")
class GetBoardEntityServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private GetBoardEntityService getBoardEntityService;

    private Board noticesBoard;
    private Board generalBoard;

    @BeforeEach
    void setUp() {
        noticesBoard = createNoticesBoard();
        generalBoard = createGeneralBoard();
    }

    @Nested
    @DisplayName("문자열 코드로 게시판 엔티티 조회")
    class GetBoardEntityByStringCodeTest {

        @DisplayName("존재하지 않는 게시판 코드 조회 시 BoardNotFoundException 발생")
        @Test
        void getBoardEntity_WithNonExistentCode_ThrowsBoardNotFoundException() {
            // given
            String nonExistentCode = "non-existent";

            // when & then
            assertThatThrownBy(() -> getBoardEntityService.getBoardEntity(nonExistentCode))
                    .isInstanceOf(BoardNotFoundException.class);
        }

        @DisplayName("존재하는 게시판 코드로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithValidCode_ReturnsBoard() {
            // given
            String validCode = "notices";
            given(boardRepository.findByCode(BoardCode.NOTICES)).willReturn(Optional.of(noticesBoard));

            // when
            Board result = getBoardEntityService.getBoardEntity(validCode);

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
            Board result = getBoardEntityService.getBoardEntity(validCode);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(BoardCode.NOTICES);
        }
    }

    @Nested
    @DisplayName("BoardCode enum으로 게시판 엔티티 조회")
    class GetBoardEntityByBoardCodeTest {

        @DisplayName("BoardCode enum으로 조회 시 게시판 반환")
        @Test
        void getBoardEntity_WithBoardCodeEnum_ReturnsBoard() {
            // given
            given(boardRepository.findByCode(BoardCode.GENERAL)).willReturn(Optional.of(generalBoard));

            // when
            Board result = getBoardEntityService.getBoardEntity(BoardCode.GENERAL);

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
            assertThatThrownBy(() -> getBoardEntityService.getBoardEntity(BoardCode.NOTICES))
                    .isInstanceOf(BoardNotFoundException.class);
        }
    }
}
