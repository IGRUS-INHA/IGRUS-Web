import apiClient from './client';

export const postsApi = {
  // 게시글 목록 조회
  getList: (board, params) => apiClient.get(`/posts/${board}`, { params }),

  // 게시글 상세 조회
  getDetail: (board, id) => apiClient.get(`/posts/${board}/${id}`),

  // 게시글 작성
  create: (board, data) => apiClient.post(`/posts/${board}`, data),

  // 게시글 수정
  update: (board, id, data) => apiClient.put(`/posts/${board}/${id}`, data),

  // 게시글 삭제
  delete: (board, id) => apiClient.delete(`/posts/${board}/${id}`),

  // 좋아요
  like: (board, id) => apiClient.post(`/posts/${board}/${id}/like`),

  // 좋아요 취소
  unlike: (board, id) => apiClient.delete(`/posts/${board}/${id}/like`),

  // 북마크
  bookmark: (board, id) => apiClient.post(`/posts/${board}/${id}/bookmark`),

  // 북마크 취소
  unbookmark: (board, id) => apiClient.delete(`/posts/${board}/${id}/bookmark`),
};

export const commentsApi = {
  // 댓글 목록 조회
  getList: (board, postId) => apiClient.get(`/comments/${board}/${postId}`),

  // 댓글 작성
  create: (board, postId, data) =>
    apiClient.post(`/comments/${board}/${postId}`, data),

  // 댓글 수정
  update: (board, id, data) => apiClient.put(`/comments/${board}/${id}`, data),

  // 댓글 삭제
  delete: (board, id) => apiClient.delete(`/comments/${board}/${id}`),
};
