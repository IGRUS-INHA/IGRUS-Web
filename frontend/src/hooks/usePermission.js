import { useAuthStore } from '@/stores';
import {
  hasPermission,
  canViewBoard,
  canWriteBoard,
  canEditPost,
  canDeletePost,
  canWriteComment,
  canRegisterEvent,
  canManageEvent,
  canAccessAdmin,
  canApproveAssociate,
  canChangeRole,
} from '@/constants/permissions';
import { ROLES } from '@/constants';

/**
 * 권한 체크 훅
 * 컴포넌트에서 권한 기반 렌더링에 사용
 */
export function usePermission() {
  const { user, isAuthenticated } = useAuthStore();
  const role = user?.role || null;
  const userId = user?.id || user?.studentId || null;

  return {
    // 기본 정보
    isAuthenticated,
    role,
    userId,

    // 일반 권한 체크
    hasPermission: (requiredRole) => hasPermission(role, requiredRole),

    // 게시판
    canViewBoard: (boardType) => canViewBoard(role, boardType),
    canWriteBoard: (boardType) => canWriteBoard(role, boardType),
    canEditPost: (postAuthorId) => canEditPost(role, userId, postAuthorId),
    canDeletePost: (postAuthorId) => canDeletePost(role, userId, postAuthorId),

    // 댓글
    canWriteComment: () => canWriteComment(role),

    // 행사
    canRegisterEvent: () => canRegisterEvent(role),
    canManageEvent: () => canManageEvent(role),

    // 관리자
    canAccessAdmin: () => canAccessAdmin(role),
    canApproveAssociate: () => canApproveAssociate(role),
    canChangeRole: () => canChangeRole(role),

    // 편의 메서드
    isAdmin: () => role === ROLES.ADMIN,
    isOperator: () => hasPermission(role, ROLES.OPERATOR),
    isMember: () => hasPermission(role, ROLES.MEMBER),
    isAssociate: () => hasPermission(role, ROLES.ASSOCIATE),
  };
}
