import apiClient from './client';

export const authApi = {
  // 회원가입
  signup: (data) => apiClient.post('/auth/signup', data),

  // 이메일 인증
  verifyEmail: (data) => apiClient.post('/auth/signup/verify', data),

  // 인증 코드 재발송
  resendCode: (email) => apiClient.post('/auth/signup/resend', { email }),

  // 로그인
  login: (data) => apiClient.post('/auth/login', data),

  // 로그아웃
  logout: () => apiClient.post('/auth/logout'),

  // 토큰 갱신
  refresh: (refreshToken) => apiClient.post('/auth/refresh', { refreshToken }),

  // 계정 복구
  recover: (data) => apiClient.post('/auth/recover', data),

  // 비밀번호 재설정 요청
  requestPasswordReset: (studentId) =>
    apiClient.post('/auth/password/reset-request', { studentId }),

  // 비밀번호 재설정
  resetPassword: (data) => apiClient.post('/auth/password/reset', data),
};
