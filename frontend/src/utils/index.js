// 날짜 포맷
export const formatDate = (dateString, options = {}) => {
  const date = new Date(dateString);
  const defaultOptions = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...options,
  };
  return date.toLocaleDateString('ko-KR', defaultOptions);
};

// 날짜+시간 포맷
export const formatDateTime = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 상대 시간 (n분 전, n시간 전)
export const formatRelativeTime = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  return formatDate(dateString);
};

// 파일 크기 포맷
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 전화번호 포맷
export const formatPhoneNumber = (phone) => {
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 11) {
    return cleaned.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
  }
  return phone;
};

// 학번 유효성 검사 (8자리 숫자)
export const isValidStudentId = (studentId) => {
  return /^\d{8}$/.test(studentId);
};

// 이메일 유효성 검사
export const isValidEmail = (email) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// 비밀번호 유효성 검사 (영문+숫자+특수문자 8자 이상)
export const isValidPassword = (password) => {
  return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(
    password
  );
};

// 텍스트 자르기
export const truncateText = (text, maxLength) => {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
};

// 쿼리스트링 파싱
export const parseQueryString = (search) => {
  return Object.fromEntries(new URLSearchParams(search));
};

// 객체를 쿼리스트링으로 변환
export const toQueryString = (params) => {
  const filtered = Object.entries(params).filter(
    ([, value]) => value !== undefined && value !== null && value !== ''
  );
  return new URLSearchParams(filtered).toString();
};

// 행사 관련 유틸
export * from './event';

// 에러 처리 유틸
export * from './error';
