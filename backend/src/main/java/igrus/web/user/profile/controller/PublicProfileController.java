package igrus.web.user.profile.controller;

import igrus.web.generated.api.UsersApi;
import igrus.web.generated.model.ApiPublicProfileResponse;
import igrus.web.user.dto.ProfileLinkMapper;
import igrus.web.user.profile.service.GetPublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 프로필 컨트롤러 — play 등 외부 표시용 (인증 불필요).
 */
@RestController
@RequiredArgsConstructor
public class PublicProfileController implements UsersApi {

    private final GetPublicProfileService getPublicProfileService;

    @Override
    public ResponseEntity<ApiPublicProfileResponse> getPublicProfile(String studentId) {
        var response = getPublicProfileService.getPublicProfile(studentId);
        return ResponseEntity.ok(new ApiPublicProfileResponse()
                .displayName(response.displayName())
                .introduction(response.introduction())
                .links(ProfileLinkMapper.toApi(response.links())));
    }
}
