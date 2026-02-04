/**
 * 동아리 정보 및 연락처 상수
 *
 * 이 파일은 IGRUS 동아리의 연락처 정보와 SNS 링크를 중앙 집중식으로 관리합니다.
 * Footer 컴포넌트와 기타 필요한 곳에서 사용됩니다.
 */

export const CLUB_INFO = {
  name: 'IGRUS',
  fullName: '인하대학교 웹 개발 동아리',
  description: '인하대학교의 웹 개발 동아리입니다.',
  // TODO: 실제 동아리 방 주소로 교체 필요
  address: '인천광역시 미추홀구 인하로 100, 인하대학교 [실제 동아리방 위치]',
  // TODO: 실제 이메일 주소로 교체 필요
  email: 'contact@igrus.club',
  // TODO: 실제 전화번호로 교체 필요
  phone: '032-860-XXXX',
} as const;

export const SNS_LINKS = {
  // TODO: 실제 인스타그램 URL로 교체 필요
  instagram: 'https://instagram.com/igrus_official',
  // TODO: 실제 블로그 URL로 교체 필요
  blog: 'https://blog.igrus.club',
} as const;

export const FOOTER_QUICK_LINKS = [
  { label: '게시판', path: '/board/notices' },
  { label: '행사', path: '/events' },
  { label: '문의', path: '/inquiry' },
] as const;

export const FOOTER_LEGAL_LINKS = [
  { label: '개인정보처리방침', path: '/privacy' },
  { label: '이용약관', path: '/terms' },
] as const;
