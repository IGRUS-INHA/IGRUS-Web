import apiClient from './client';

export const eventsApi = {
  // 행사 목록 조회
  getList: (params) => apiClient.get('/events', { params }),

  // 행사 상세 조회
  getDetail: (id) => apiClient.get(`/events/${id}`),

  // 행사 생성 (OPERATOR 이상)
  create: (data) => apiClient.post('/events', data),

  // 행사 수정 (OPERATOR 이상)
  update: (id, data) => apiClient.put(`/events/${id}`, data),

  // 행사 삭제 (OPERATOR 이상)
  delete: (id) => apiClient.delete(`/events/${id}`),

  // 행사 신청 (MEMBER 이상)
  register: (id) => apiClient.post(`/events/${id}/register`),

  // 행사 신청 취소 (MEMBER 이상)
  cancelRegistration: (id) => apiClient.delete(`/events/${id}/register`),

  // 조기 마감 (OPERATOR 이상)
  close: (id) => apiClient.post(`/events/${id}/close`),

  // 신청자 목록 (OPERATOR 이상)
  getRegistrations: (id) => apiClient.get(`/events/${id}/registrations`),

  // 신청자 엑셀 다운로드 (OPERATOR 이상)
  exportRegistrations: (id) =>
    apiClient.get(`/events/${id}/registrations/export`, {
      responseType: 'blob',
    }),
};
