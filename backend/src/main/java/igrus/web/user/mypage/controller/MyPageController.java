package igrus.web.user.mypage.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostPageResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.service.read.GetMyBookmarksService;
import igrus.web.community.like.post_like.dto.response.LikedPostPageResponse;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.service.read.GetMyLikedPostsService;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.mypage.dto.request.ChangePasswordRequest;
import igrus.web.user.mypage.dto.request.UpdateProfileRequest;
import igrus.web.user.mypage.dto.response.MyCommentPageResponse;
import igrus.web.user.mypage.dto.response.MyCommentResponse;
import igrus.web.user.mypage.dto.response.MyPostPageResponse;
import igrus.web.user.mypage.dto.response.MyPostResponse;
import igrus.web.user.mypage.dto.response.MyProfileResponse;
import igrus.web.user.mypage.service.read.GetMyCommentsService;
import igrus.web.user.mypage.service.read.GetMyPostsService;
import igrus.web.user.mypage.service.read.GetMyProfileService;
import igrus.web.user.mypage.service.write.ChangeMyPasswordService;
import igrus.web.user.mypage.service.write.UpdateMyProfileService;
import igrus.web.user.withdrawal.dto.request.WithdrawRequest;
import igrus.web.user.withdrawal.service.WithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 마이페이지 컨트롤러.
 * 프로필 조회/수정, 비밀번호 변경, 내 활동 조회 API를 제공합니다.
 */
@Tag(name = "MyPage", description = "마이페이지 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final GetMyProfileService getMyProfileService;
    private final GetMyPostsService getMyPostsService;
    private final GetMyCommentsService getMyCommentsService;
    private final EventRegistrationService eventRegistrationService;
    private final GetMyLikedPostsService getMyLikedPostsService;
    private final GetMyBookmarksService getMyBookmarksService;
    private final UpdateMyProfileService updateMyProfileService;
    private final ChangeMyPasswordService changeMyPasswordService;
    private final WithdrawService withdrawService;

    // === 프로필 ===

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MyProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/profile")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MyProfileResponse response = getMyProfileService.getMyProfile(user.userId());
        return ResponseEntity.ok(response);
    }

    // === 프로필 수정 ===

    @Operation(summary = "프로필 수정", description = "이메일, 전화번호를 수정합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값 (이메일 형식 오류, 전화번호 형식 오류)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이메일 또는 전화번호 중복",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        updateMyProfileService.updateProfile(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 비밀번호 변경 ===

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "새 비밀번호 형식 오류 또는 현재 비밀번호와 동일",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "현재 비밀번호 불일치 또는 인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        changeMyPasswordService.changePassword(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 회원 탈퇴 ===

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 회원 탈퇴를 진행합니다. 탈퇴 후 5일 이내 복구 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "비밀번호 불일치 또는 인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/account")
    public ResponseEntity<Void> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        withdrawService.withdraw(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 내 활동 조회 ===

    @Operation(summary = "내 게시글 목록 조회", description = "내가 작성한 게시글 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/posts")
    public ResponseEntity<MyPostPageResponse> getMyPosts(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Page<MyPostResponse> page = getMyPostsService.getMyPosts(user.userId(), pageable);
        return ResponseEntity.ok(MyPostPageResponse.from(page));
    }

    @Operation(summary = "내 댓글 목록 조회", description = "내가 작성한 댓글 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/comments")
    public ResponseEntity<MyCommentPageResponse> getMyComments(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Page<MyCommentResponse> page = getMyCommentsService.getMyComments(user.userId(), pageable);
        return ResponseEntity.ok(MyCommentPageResponse.from(page));
    }

    @Operation(summary = "내 행사 신청 목록 조회", description = "내가 신청한 행사 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 신청 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/registrations")
    public ResponseEntity<List<MyRegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<MyRegistrationResponse> response = eventRegistrationService.getMyRegistrations(user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "좋아요한 게시글 목록 조회", description = "내가 좋아요한 게시글 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요한 게시글 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/likes")
    public ResponseEntity<LikedPostPageResponse> getMyLikes(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Page<LikedPostResponse> page = getMyLikedPostsService.getMyLikes(user.userId(), pageable);
        return ResponseEntity.ok(LikedPostPageResponse.from(page));
    }

    @Operation(summary = "북마크한 게시글 목록 조회", description = "내가 북마크한 게시글 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "북마크한 게시글 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/bookmarks")
    public ResponseEntity<BookmarkedPostPageResponse> getMyBookmarks(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Page<BookmarkedPostResponse> page = getMyBookmarksService.getMyBookmarks(user.userId(), pageable);
        return ResponseEntity.ok(BookmarkedPostPageResponse.from(page));
    }

}
