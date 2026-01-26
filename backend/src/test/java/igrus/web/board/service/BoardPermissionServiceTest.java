package igrus.web.board.service;

import igrus.web.board.domain.Board;
import igrus.web.board.domain.BoardCode;
import igrus.web.board.domain.BoardPermission;
import igrus.web.board.exception.BoardReadDeniedException;
import igrus.web.board.exception.BoardWriteDeniedException;
import igrus.web.board.repository.BoardPermissionRepository;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardPermissionService 단위 테스트")
class BoardPermissionServiceTest {

    @Mock
    private BoardPermissionRepository boardPermissionRepository;

    @InjectMocks
    private BoardPermissionService boardPermissionService;

    private Board noticeBoard;
    private Board freeBoard;

    @BeforeEach
    void setUp() {
        noticeBoard = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
        freeBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유게시판", false, false, 2);
    }

    @Nested
    @DisplayName("canRead 메서드")
    class CanReadTest {

        @DisplayName("준회원이 공지사항 읽기 권한 확인 - true")
        @Test
        void canRead_AssociateWithNoticeBoard_ReturnsTrue() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.ASSOCIATE, true, false);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.of(permission));

            // when
            boolean result = boardPermissionService.canRead(noticeBoard, UserRole.ASSOCIATE);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("준회원이 자유게시판 읽기 권한 확인 - false")
        @Test
        void canRead_AssociateWithFreeBoard_ReturnsFalse() {
            // given
            BoardPermission permission = BoardPermission.create(freeBoard, UserRole.ASSOCIATE, false, false);
            given(boardPermissionRepository.findByBoardAndRole(freeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.of(permission));

            // when
            boolean result = boardPermissionService.canRead(freeBoard, UserRole.ASSOCIATE);

            // then
            assertThat(result).isFalse();
        }

        @DisplayName("권한 정보가 없는 경우 - false")
        @Test
        void canRead_WithNoPermission_ReturnsFalse() {
            // given
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.empty());

            // when
            boolean result = boardPermissionService.canRead(noticeBoard, UserRole.ASSOCIATE);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("canWrite 메서드")
    class CanWriteTest {

        @DisplayName("정회원이 공지사항 쓰기 권한 확인 - false")
        @Test
        void canWrite_MemberWithNoticeBoard_ReturnsFalse() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.MEMBER, true, false);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                    .willReturn(Optional.of(permission));

            // when
            boolean result = boardPermissionService.canWrite(noticeBoard, UserRole.MEMBER);

            // then
            assertThat(result).isFalse();
        }

        @DisplayName("OPERATOR가 공지사항 쓰기 권한 확인 - true")
        @Test
        void canWrite_OperatorWithNoticeBoard_ReturnsTrue() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.OPERATOR, true, true);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.OPERATOR))
                    .willReturn(Optional.of(permission));

            // when
            boolean result = boardPermissionService.canWrite(noticeBoard, UserRole.OPERATOR);

            // then
            assertThat(result).isTrue();
        }

        @DisplayName("권한 정보가 없는 경우 - false")
        @Test
        void canWrite_WithNoPermission_ReturnsFalse() {
            // given
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                    .willReturn(Optional.empty());

            // when
            boolean result = boardPermissionService.canWrite(noticeBoard, UserRole.MEMBER);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("checkReadPermission 메서드")
    class CheckReadPermissionTest {

        @DisplayName("읽기 권한이 있는 경우 예외가 발생하지 않음")
        @Test
        void checkReadPermission_WithValidPermission_NoException() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.ASSOCIATE, true, false);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.of(permission));

            // when & then
            boardPermissionService.checkReadPermission(noticeBoard, UserRole.ASSOCIATE);
        }

        @DisplayName("읽기 권한 없는 접근 시 BoardReadDeniedException 발생")
        @Test
        void checkReadPermission_WithoutPermission_ThrowsBoardReadDeniedException() {
            // given
            BoardPermission permission = BoardPermission.create(freeBoard, UserRole.ASSOCIATE, false, false);
            given(boardPermissionRepository.findByBoardAndRole(freeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.of(permission));

            // getRequiredRoleForRead 내부에서 호출되는 canRead를 위한 추가 stubbing
            for (UserRole role : UserRole.values()) {
                if (role != UserRole.ASSOCIATE) {
                    given(boardPermissionRepository.findByBoardAndRole(freeBoard, role))
                            .willReturn(Optional.empty());
                }
            }

            // when & then
            assertThatThrownBy(() -> boardPermissionService.checkReadPermission(freeBoard, UserRole.ASSOCIATE))
                    .isInstanceOf(BoardReadDeniedException.class);
        }

        @DisplayName("권한 정보가 없는 경우 BoardReadDeniedException 발생")
        @Test
        void checkReadPermission_WithNoPermissionRecord_ThrowsBoardReadDeniedException() {
            // given
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.ASSOCIATE))
                    .willReturn(Optional.empty());

            // getRequiredRoleForRead 내부에서 호출되는 canRead를 위한 추가 stubbing
            for (UserRole role : UserRole.values()) {
                if (role != UserRole.ASSOCIATE) {
                    given(boardPermissionRepository.findByBoardAndRole(noticeBoard, role))
                            .willReturn(Optional.empty());
                }
            }

            // when & then
            assertThatThrownBy(() -> boardPermissionService.checkReadPermission(noticeBoard, UserRole.ASSOCIATE))
                    .isInstanceOf(BoardReadDeniedException.class);
        }
    }

    @Nested
    @DisplayName("checkWritePermission 메서드")
    class CheckWritePermissionTest {

        @DisplayName("쓰기 권한이 있는 경우 예외가 발생하지 않음")
        @Test
        void checkWritePermission_WithValidPermission_NoException() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.OPERATOR, true, true);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.OPERATOR))
                    .willReturn(Optional.of(permission));

            // when & then
            boardPermissionService.checkWritePermission(noticeBoard, UserRole.OPERATOR);
        }

        @DisplayName("쓰기 권한 없는 접근 시 BoardWriteDeniedException 발생")
        @Test
        void checkWritePermission_WithoutPermission_ThrowsBoardWriteDeniedException() {
            // given
            BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.MEMBER, true, false);
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                    .willReturn(Optional.of(permission));

            // getRequiredRoleForWrite 내부에서 호출되는 canWrite를 위한 추가 stubbing
            for (UserRole role : UserRole.values()) {
                if (role != UserRole.MEMBER) {
                    given(boardPermissionRepository.findByBoardAndRole(noticeBoard, role))
                            .willReturn(Optional.empty());
                }
            }

            // when & then
            assertThatThrownBy(() -> boardPermissionService.checkWritePermission(noticeBoard, UserRole.MEMBER))
                    .isInstanceOf(BoardWriteDeniedException.class);
        }

        @DisplayName("권한 정보가 없는 경우 BoardWriteDeniedException 발생")
        @Test
        void checkWritePermission_WithNoPermissionRecord_ThrowsBoardWriteDeniedException() {
            // given
            given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.MEMBER))
                    .willReturn(Optional.empty());

            // getRequiredRoleForWrite 내부에서 호출되는 canWrite를 위한 추가 stubbing
            for (UserRole role : UserRole.values()) {
                if (role != UserRole.MEMBER) {
                    given(boardPermissionRepository.findByBoardAndRole(noticeBoard, role))
                            .willReturn(Optional.empty());
                }
            }

            // when & then
            assertThatThrownBy(() -> boardPermissionService.checkWritePermission(noticeBoard, UserRole.MEMBER))
                    .isInstanceOf(BoardWriteDeniedException.class);
        }
    }
}
