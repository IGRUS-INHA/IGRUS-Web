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
import { ROLES, type RoleOrNull, type BoardType } from '@/types/common';

interface UsePermissionReturn {
  // 기본 정보
  isAuthenticated: boolean;
  role: RoleOrNull;
  userId: string | null;

  // 일반 권한 체크
  hasPermission: (requiredRole: RoleOrNull) => boolean;

  // 게시판
  canViewBoard: (boardType: BoardType) => boolean;
  canWriteBoard: (boardType: BoardType) => boolean;
  canEditPost: (postAuthorId: string) => boolean;
  canDeletePost: (postAuthorId: string) => boolean;

  // 댓글
  canWriteComment: () => boolean;

  // 행사
  canRegisterEvent: () => boolean;
  canManageEvent: () => boolean;

  // 관리자
  canAccessAdmin: () => boolean;
  canApproveAssociate: () => boolean;
  canChangeRole: () => boolean;

  // 편의 메서드
  isAdmin: () => boolean;
  isOperator: () => boolean;
  isMember: () => boolean;
  isAssociate: () => boolean;
}

/**
 * 권한 체크 훅
 * 컴포넌트에서 권한 기반 렌더링에 사용
 */
export function usePermission(): UsePermissionReturn {
  const { user, isAuthenticated } = useAuthStore();
  const role: RoleOrNull = user?.role ?? null;
  const userId: string | null = user?.id ?? user?.studentId ?? null;

  return {
    // 기본 정보
    isAuthenticated,
    role,
    userId,

    // 일반 권한 체크
    hasPermission: (requiredRole: RoleOrNull): boolean =>
      hasPermission(role, requiredRole),

    // 게시판
    canViewBoard: (boardType: BoardType): boolean =>
      canViewBoard(role, boardType),
    canWriteBoard: (boardType: BoardType): boolean =>
      canWriteBoard(role, boardType),
    canEditPost: (postAuthorId: string): boolean =>
      canEditPost(role, userId, postAuthorId),
    canDeletePost: (postAuthorId: string): boolean =>
      canDeletePost(role, userId, postAuthorId),

    // 댓글
    canWriteComment: (): boolean => canWriteComment(role),

    // 행사
    canRegisterEvent: (): boolean => canRegisterEvent(role),
    canManageEvent: (): boolean => canManageEvent(role),

    // 관리자
    canAccessAdmin: (): boolean => canAccessAdmin(role),
    canApproveAssociate: (): boolean => canApproveAssociate(role),
    canChangeRole: (): boolean => canChangeRole(role),

    // 편의 메서드
    isAdmin: (): boolean => role === ROLES.ADMIN,
    isOperator: (): boolean => hasPermission(role, ROLES.OPERATOR),
    isMember: (): boolean => hasPermission(role, ROLES.MEMBER),
    isAssociate: (): boolean => hasPermission(role, ROLES.ASSOCIATE),
  };
}
