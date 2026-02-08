export const PAGE_TITLES: Record<string, string> = {
  '/': 'Home',
  '/login': ' ',
  '/signup': ' ',
  '/verify-email': 'Email Verification',
  '/forgot-password': 'Forgot Password',
  '/reset-password': 'Reset Password',
  '/board/notices': 'Notices',
  '/board/general': 'Community',
  '/board/insight': 'Insights',
  '/events': 'Events',
  '/inquiry': ' ',
  '/inquiry/lookup': 'Inquiry Lookup',
  '/mypage': 'My Page',
  '/admin': 'Admin Dashboard',
};

export function getPageTitle(pathname: string): string {
  // 게시판 경로는 모두 Community로 표시
  if (pathname.startsWith('/board')) {
    return 'Community';
  }

  // 정확한 매칭 우선
  if (PAGE_TITLES[pathname]) {
    return PAGE_TITLES[pathname];
  }

  // 동적 라우트 처리
  const segments = pathname.split('/').filter(Boolean);

  // /events/:eventId (행사 상세)
  if (segments[0] === 'events' && segments.length === 2) {
    return 'Event Detail';
  }

  // /mypage/* (마이페이지 하위 경로)
  if (pathname.startsWith('/mypage')) {
    return 'My Page';
  }

  // Fallback
  return 'Page';
}
