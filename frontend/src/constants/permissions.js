import { ROLES, BOARDS } from './index';

// 역할 계층 (null = 비회원)
const ROLE_HIERARCHY = [null, ROLES.ASSOCIATE, ROLES.MEMBER, ROLES.OPERATOR, ROLES.ADMIN];

// 기능별 최소 권한 정의
export const PERMISSIONS = {
  // 게시판 조회
  BOARD_VIEW: {
    [BOARDS.NOTICES]: null, // 비회원 가능
    [BOARDS.GENERAL]: ROLES.ASSOCIATE, // 준회원 이상
    [BOARDS.INSIGHT]: ROLES.ASSOCIATE, // 준회원 이상
  },

  // 게시판 글쓰기
  BOARD_WRITE: {
    [BOARDS.NOTICES]: ROLES.OPERATOR, // 공지는 운영진만
    [BOARDS.GENERAL]: ROLES.MEMBER, // 정회원 이상
    [BOARDS.INSIGHT]: ROLES.MEMBER, // 정회원 이상
  },

  // 행사
  EVENT_VIEW: null, // 비회원 가능
  EVENT_REGISTER: ROLES.MEMBER, // 정회원 이상
  EVENT_MANAGE: ROLES.OPERATOR, // 운영진 이상

  // 댓글
  COMMENT_WRITE: ROLES.MEMBER, // 정회원 이상

  // 관리자
  ADMIN_DASHBOARD: ROLES.OPERATOR,
  ADMIN_USERS: ROLES.OPERATOR,
  ADMIN_INQUIRIES: ROLES.OPERATOR,
  ADMIN_APPROVE: ROLES.ADMIN, // 준회원 승인
  ADMIN_ROLE_CHANGE: ROLES.ADMIN, // 권한 변경
};

/**
 * 권한 체크
 * @param {string|null} userRole - 사용자 역할 (null이면 비회원)
 * @param {string|null} requiredRole - 필요 역할 (null이면 비회원도 가능)
 * @returns {boolean}
 */
export function hasPermission(userRole, requiredRole) {
  // 비회원도 가능한 기능
  if (requiredRole === null) return true;
  // 비회원인데 권한 필요
  if (!userRole) return false;

  const userIndex = ROLE_HIERARCHY.indexOf(userRole);
  const requiredIndex = ROLE_HIERARCHY.indexOf(requiredRole);

  return userIndex >= requiredIndex;
}

/**
 * 게시판 조회 권한
 */
export function canViewBoard(userRole, boardType) {
  return hasPermission(userRole, PERMISSIONS.BOARD_VIEW[boardType]);
}

/**
 * 게시판 글쓰기 권한
 */
export function canWriteBoard(userRole, boardType) {
  return hasPermission(userRole, PERMISSIONS.BOARD_WRITE[boardType]);
}

/**
 * 게시글 수정 권한 (본인 글 또는 운영진)
 */
export function canEditPost(userRole, userId, postAuthorId) {
  if (!userRole || !userId) return false;
  // 본인 글
  if (userId === postAuthorId) return true;
  // 운영진 이상
  return hasPermission(userRole, ROLES.OPERATOR);
}

/**
 * 게시글 삭제 권한 (본인 글 또는 운영진)
 */
export function canDeletePost(userRole, userId, postAuthorId) {
  return canEditPost(userRole, userId, postAuthorId);
}

/**
 * 댓글 작성 권한
 */
export function canWriteComment(userRole) {
  return hasPermission(userRole, PERMISSIONS.COMMENT_WRITE);
}

/**
 * 행사 신청 권한
 */
export function canRegisterEvent(userRole) {
  return hasPermission(userRole, PERMISSIONS.EVENT_REGISTER);
}

/**
 * 행사 관리 권한 (생성/수정/삭제/참가자관리)
 */
export function canManageEvent(userRole) {
  return hasPermission(userRole, PERMISSIONS.EVENT_MANAGE);
}

/**
 * 관리자 대시보드 접근 권한
 */
export function canAccessAdmin(userRole) {
  return hasPermission(userRole, PERMISSIONS.ADMIN_DASHBOARD);
}

/**
 * 준회원 승인 권한
 */
export function canApproveAssociate(userRole) {
  return hasPermission(userRole, PERMISSIONS.ADMIN_APPROVE);
}

/**
 * 회원 권한 변경 권한
 */
export function canChangeRole(userRole) {
  return hasPermission(userRole, PERMISSIONS.ADMIN_ROLE_CHANGE);
}
