package igrus.web.user.profile.dto.response;

import igrus.web.user.domain.ProfileLink;
import igrus.web.user.domain.User;

import java.util.List;

/**
 * 공개 프로필 응답 DTO — play 등 외부 표시용.
 * 닉네임이 있으면 실명은 포함하지 않는다 (서버단 폴백).
 */
public record PublicProfileResponse(
        String displayName,

        String introduction,

        List<ProfileLink> links
) {
    public static PublicProfileResponse from(User user) {
        return new PublicProfileResponse(
                user.getPublicDisplayName(),
                user.getIntroduction(),
                user.getLinks()
        );
    }
}
