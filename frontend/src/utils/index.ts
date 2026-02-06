// 날짜 포맷
export function formatDate(
  dateString: string | Date,
  options?: Intl.DateTimeFormatOptions
): string {
  const date = new Date(dateString);
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...options,
  };
  return date.toLocaleDateString('ko-KR', defaultOptions);
}

// 헤더용 날짜 포맷 (Wednesday, 24 May 2024)
export function formatHeaderDate(date: Date = new Date()): string {
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

// 날짜+시간 포맷
export function formatDateTime(dateString: string | Date): string {
  const date = new Date(dateString);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// 상대 시간 (n분 전, n시간 전)
export function formatRelativeTime(dateString: string | Date): string {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  return formatDate(dateString);
}

// 파일 크기 포맷
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'] as const;
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 전화번호 포맷
export function formatPhoneNumber(phone: string): string {
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 11) {
    return cleaned.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
  }
  return phone;
}

// 학번 유효성 검사 (8자리 숫자)
export function isValidStudentId(studentId: string): boolean {
  return /^\d{8}$/.test(studentId);
}

// 이메일 유효성 검사
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// 비밀번호 유효성 검사 (영문+숫자+특수문자 8자 이상)
export function isValidPassword(password: string): boolean {
  return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(
    password
  );
}

// 텍스트 자르기
export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
}

// 쿼리스트링 파싱
export function parseQueryString(search: string): Record<string, string> {
  return Object.fromEntries(new URLSearchParams(search));
}

// 객체를 쿼리스트링으로 변환
export function toQueryString(params: Record<string, unknown>): string {
  const filtered = Object.entries(params).filter(
    ([, value]) => value !== undefined && value !== null && value !== ''
  );
  return new URLSearchParams(
    filtered.map(([key, value]) => [key, String(value)])
  ).toString();
}

// 행사 관련 유틸
export * from './event';

// 에러 처리 유틸
export * from './error';
