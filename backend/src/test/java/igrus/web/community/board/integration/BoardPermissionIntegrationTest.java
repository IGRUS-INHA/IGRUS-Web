package igrus.web.community.board.integration;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시판 권한 검증 통합 테스트.
 *
 * <p>User Story 2의 권한 관리 기능을 검증합니다:</p>
 * <ul>
 *     <li>준회원이 자유게시판 접근 시 403 Forbidden</li>
 *     <li>정회원이 모든 게시판 접근 성공</li>
 *     <li>OPERATOR가 공지사항 쓰기 권한 보유</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("BoardPermission 통합 테스트")
class BoardPermissionIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    private static final String BASE_URL = "/api/v1/boards";

    private User associateUser;
    private User memberUser;
    private User operatorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        setupUsers();
    }

    private void setupBoardData() {
        // 게시판 생성
        Board notices = Board.create(BoardCode.NOTICES, "공지사항", "동아리 공지사항을 확인할 수 있습니다.", false, false, 1);
        Board general = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        Board insight = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(notices);
        boardRepository.save(general);
        boardRepository.save(insight);

        // 권한 설정 - notices: 준회원 읽기만, 정회원 읽기만, OPERATOR/ADMIN 읽기+쓰기
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.ASSOCIATE, true, false));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.MEMBER, true, false));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.ADMIN, true, true));

        // 권한 설정 - general: 준회원 접근 불가, 정회원 이상 읽기+쓰기
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.ADMIN, true, true));

        // 권한 설정 - insight: 준회원 접근 불가, 정회원 이상 읽기+쓰기
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.ADMIN, true, true));
    }

    private void setupUsers() {
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        operatorUser = createAndSaveUser("20180001", "operator@inha.edu", UserRole.OPERATOR);
        adminUser = createAndSaveUser("20150001", "admin@inha.edu", UserRole.ADMIN);
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    @Nested
    @DisplayName("준회원 접근 제한 테스트")
    class AssociateAccessRestrictionTest {

        @DisplayName("준회원이 공지사항 조회 시 성공 (읽기 권한 있음)")
        @Test
        void associate_canRead_notices() throws Exception {
            mockMvc.perform(get(BASE_URL + "/notices")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("NOTICES"))
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(false));
        }

        @DisplayName("준회원이 자유게시판 조회 시 권한 정보에 canRead=false 반환")
        @Test
        void associate_cannotRead_general_returnsCanReadFalse() throws Exception {
            mockMvc.perform(get(BASE_URL + "/general")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("GENERAL"))
                    .andExpect(jsonPath("$.canRead").value(false))
                    .andExpect(jsonPath("$.canWrite").value(false));
        }

        @DisplayName("준회원이 게시판 목록 조회 시 읽기 권한 있는 게시판만 반환")
        @Test
        void associate_getBoardList_returnsOnlyAccessibleBoards() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value("NOTICES"));
        }
    }

    @Nested
    @DisplayName("정회원 접근 테스트")
    class MemberAccessTest {

        @DisplayName("정회원이 모든 게시판 조회 성공")
        @Test
        void member_canRead_allBoards() throws Exception {
            // 공지사항 조회
            mockMvc.perform(get(BASE_URL + "/notices")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(false));

            // 자유게시판 조회
            mockMvc.perform(get(BASE_URL + "/general")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(true));

            // 정보공유 조회
            mockMvc.perform(get(BASE_URL + "/insight")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(true));
        }

        @DisplayName("정회원이 게시판 목록 조회 시 모든 게시판 반환")
        @Test
        void member_getBoardList_returnsAllBoards() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].code").value("NOTICES"))
                    .andExpect(jsonPath("$[1].code").value("GENERAL"))
                    .andExpect(jsonPath("$[2].code").value("INSIGHT"));
        }

        @DisplayName("정회원이 공지사항 쓰기 권한 없음")
        @Test
        void member_cannotWrite_notices() throws Exception {
            mockMvc.perform(get(BASE_URL + "/notices")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canWrite").value(false));
        }
    }

    @Nested
    @DisplayName("OPERATOR 권한 테스트")
    class OperatorAccessTest {

        @DisplayName("OPERATOR가 공지사항 쓰기 권한 보유")
        @Test
        void operator_canWrite_notices() throws Exception {
            mockMvc.perform(get(BASE_URL + "/notices")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canRead").value(true))
                    .andExpect(jsonPath("$.canWrite").value(true));
        }

        @DisplayName("OPERATOR가 모든 게시판에 읽기/쓰기 권한 보유")
        @Test
        void operator_hasFullAccess_allBoards() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3));

            // 각 게시판별 권한 확인
            String[] boards = {"notices", "general", "insight"};
            for (String boardCode : boards) {
                mockMvc.perform(get(BASE_URL + "/" + boardCode)
                                .with(withAuth(operatorUser))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canRead").value(true))
                        .andExpect(jsonPath("$.canWrite").value(true));
            }
        }
    }

    @Nested
    @DisplayName("ADMIN 권한 테스트")
    class AdminAccessTest {

        @DisplayName("ADMIN이 모든 게시판에 전체 권한 보유")
        @Test
        void admin_hasFullAccess_allBoards() throws Exception {
            String[] boards = {"notices", "general", "insight"};
            for (String boardCode : boards) {
                mockMvc.perform(get(BASE_URL + "/" + boardCode)
                                .with(withAuth(adminUser))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.canRead").value(true))
                        .andExpect(jsonPath("$.canWrite").value(true));
            }
        }
    }

    @Nested
    @DisplayName("비인증 사용자 접근 테스트")
    class UnauthenticatedAccessTest {

        @DisplayName("비인증 사용자가 게시판 목록 조회 시 403 Forbidden")
        @Test
        void unauthenticated_getBoardList_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("비인증 사용자가 게시판 상세 조회 시 403 Forbidden")
        @Test
        void unauthenticated_getBoardDetail_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL + "/notices")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("존재하지 않는 게시판 테스트")
    class NonExistentBoardTest {

        @DisplayName("존재하지 않는 게시판 코드로 조회 시 404 Not Found")
        @Test
        void getBoardByCode_withInvalidCode_returnsNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/invalid-board")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_NOT_FOUND.getCode()));
        }
    }
}
