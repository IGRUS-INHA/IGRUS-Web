export const PAGE_TITLES: Record<string, string> = {
  '/': 'Home',
  '/login': 'Login',
  '/signup': 'Sign Up',
  '/verify-email': 'Email Verification',
  '/forgot-password': 'Forgot Password',
  '/reset-password': 'Reset Password',
  '/board/notices': 'Notices',
  '/board/general': 'Community',
  '/board/insight': 'Insights',
  '/events': 'Events',
  '/inquiry': 'Inquiry',
  '/inquiry/lookup': 'Inquiry Lookup',
  '/mypage': 'My Page',
  '/admin': 'Admin Dashboard',
  '/admin/users': 'User Management',
  '/admin/associates': 'Associate Management',
  '/admin/inquiries': 'Inquiry Management',
  '/admin/scraps': 'Scrap Management',
};

export function getPageTitle(pathname: string): string {
  // 정확한 매칭 우선
  if (PAGE_TITLES[pathname]) {
    return PAGE_TITLES[pathname];
  }

  // 동적 라우트 처리
  const segments = pathname.split('/').filter(Boolean);

  // /board/:boardType/:postId (게시글 상세)
  if (segments[0] === 'board' && segments.length >= 2) {
    const boardPath = `/${segments[0]}/${segments[1]}`;
    return PAGE_TITLES[boardPath] || 'Post';
  }

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
