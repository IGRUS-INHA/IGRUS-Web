import apiClient from './client';

export const usersApi = {
  // 내 정보 조회
  getMe: (userId) => apiClient.get(`/users/${userId}`),

  // 정보 수정
  update: (userId, data) => apiClient.put(`/users/${userId}`, data),

  // 비밀번호 변경
  updatePassword: (userId, data) =>
    apiClient.put(`/users/${userId}/password`, data),

  // 회원 탈퇴
  withdraw: (userId, data) => apiClient.delete(`/users/${userId}`, { data }),

  // 내 게시글 목록
  getMyPosts: (userId, params) =>
    apiClient.get(`/users/${userId}/posts`, { params }),

  // 내 댓글 목록
  getMyComments: (userId, params) =>
    apiClient.get(`/users/${userId}/comments`, { params }),

  // 좋아요한 게시글 목록
  getMyLikes: (userId, params) =>
    apiClient.get(`/users/${userId}/likes`, { params }),

  // 북마크한 게시글 목록
  getMyBookmarks: (userId, params) =>
    apiClient.get(`/users/${userId}/bookmarks`, { params }),

  // 신청한 행사 목록
  getMyEvents: (userId, params) =>
    apiClient.get(`/users/${userId}/events`, { params }),
};
