export const PAGE_TITLES: Record<string, string> = {
  "/": "Home",
  "/login": " ",
  "/signup": " ",
  "/forgot-password": "Forgot Password",
  "/reset-password": "Reset Password",
  "/board/notices": "Notices",
  ...(__FEATURE_COMMUNITY__
    ? {
        "/board/general": "Community",
        "/board/insight": "Insights",
      }
    : {}),
  "/events": "Events",
  "/inquiry": " ",
  "/inquiry/lookup": "Inquiry Lookup",
  "/mypage": "My Page",
  "/admin": "Admin Dashboard",
};

export function getPageTitle(pathname: string): string {
  // 게시판 경로 타이틀
  if (pathname.startsWith("/board")) {
    return __FEATURE_COMMUNITY__ ? "Community" : "Notices";
  }

  // 정확한 매칭 우선
  if (PAGE_TITLES[pathname]) {
    return PAGE_TITLES[pathname];
  }

  // 동적 라우트 처리
  const segments = pathname.split("/").filter(Boolean);

  // /events/:eventId/registrations (신청자 관리)
  if (
    segments[0] === "events" &&
    segments.length === 3 &&
    segments[2] === "registrations"
  ) {
    return " ";
  }

  // /events/:eventId (행사 상세)
  if (segments[0] === "events" && segments.length === 2) {
    return "Event Detail";
  }

  // /mypage/* (마이페이지 하위 경로)
  if (pathname.startsWith("/mypage")) {
    return "My Page";
  }

  // Fallback
  return "Page";
}
