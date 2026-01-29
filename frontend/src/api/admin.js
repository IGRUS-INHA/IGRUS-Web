import apiClient from './client';

export const adminApi = {
  // 대시보드 통계 (OPERATOR 이상)
  getDashboard: () => apiClient.get('/admin/dashboard'),

  // 승인 대기 준회원 목록 (ADMIN)
  getAssociates: (params) => apiClient.get('/admin/associates', { params }),

  // 준회원 승인 (ADMIN)
  approveAssociate: (id) => apiClient.post(`/admin/associates/${id}/approve`),

  // 준회원 일괄 승인 (ADMIN)
  approveAssociatesBatch: (ids) =>
    apiClient.post('/admin/associates/approve-batch', { ids }),

  // 회원 목록 (OPERATOR 이상)
  getUsers: (params) => apiClient.get('/admin/users', { params }),

  // 회원 상세 (OPERATOR 이상)
  getUser: (id) => apiClient.get(`/admin/users/${id}`),

  // 권한 변경 (ADMIN)
  updateUserRole: (id, role) =>
    apiClient.put(`/admin/users/${id}/role`, { role }),

  // 상태 변경 (ADMIN)
  updateUserStatus: (id, data) =>
    apiClient.put(`/admin/users/${id}/status`, data),

  // 강제 탈퇴 (ADMIN)
  deleteUser: (id, reason) =>
    apiClient.delete(`/admin/users/${id}`, { data: { reason } }),

  // 명단 업로드 (ADMIN)
  uploadMembers: (file, semester) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('semester', semester);
    return apiClient.post('/admin/members/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
