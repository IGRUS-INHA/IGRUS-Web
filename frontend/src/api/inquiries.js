import apiClient from './client';

export const inquiriesApi = {
  // 문의 작성
  create: (data) => apiClient.post('/inquiries', data),

  // 내 문의 목록 (로그인 사용자)
  getMyList: (params) => apiClient.get('/inquiries/my', { params }),

  // 내 문의 상세 (로그인 사용자)
  getMyDetail: (id) => apiClient.get(`/inquiries/my/${id}`),

  // 비회원 문의 조회
  lookup: (data) => apiClient.post('/inquiries/lookup', data),

  // 문의 목록 (OPERATOR 이상)
  getList: (params) => apiClient.get('/inquiries', { params }),

  // 문의 상세 (OPERATOR 이상)
  getDetail: (id) => apiClient.get(`/inquiries/${id}`),

  // 상태 변경 (OPERATOR 이상)
  updateStatus: (id, status) =>
    apiClient.put(`/inquiries/${id}/status`, { status }),

  // 답변 작성 (OPERATOR 이상)
  reply: (id, data) => apiClient.post(`/inquiries/${id}/reply`, data),

  // 내부 메모 추가 (OPERATOR 이상)
  addMemo: (id, content) =>
    apiClient.post(`/inquiries/${id}/memo`, { content }),
};
