package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CanReadBoardService 단위 테스트")
class CanReadBoardServiceTest {

    @Mock
    private BoardPermissionRepository boardPermissionRepository;

    @InjectMocks
    private CanReadBoardService canReadBoardService;

    private Board noticeBoard;
    private Board freeBoard;

    @BeforeEach
    void setUp() {
        noticeBoard = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
        freeBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유게시판", false, false, 2);
    }

    @DisplayName("준회원이 공지사항 읽기 권한 확인 - true")
    @Test
    void canRead_AssociateWithNoticeBoard_ReturnsTrue() {
        // given
        BoardPermission permission = BoardPermission.create(noticeBoard, UserRole.ASSOCIATE, true, false);
        given(boardPermissionRepository.findByBoardAndRole(noticeBoard, UserRole.ASSOCIATE))
                .willReturn(Optional.of(permission));

        // when
        boolean result = canReadBoardService.canRead(noticeBoard, UserRole.ASSOCIATE);

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
        boolean result = canReadBoardService.canRead(freeBoard, UserRole.ASSOCIATE);

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
        boolean result = canReadBoardService.canRead(noticeBoard, UserRole.ASSOCIATE);

        // then
        assertThat(result).isFalse();
    }
}
