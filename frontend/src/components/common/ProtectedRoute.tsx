import { useEffect, useRef } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUIStore } from '@/stores';
import { useAuth } from '@/hooks';
import { hasPermission } from '@/constants/permissions';
import { ROLE_LABELS } from '@/constants';
import { FullPageSpinner } from '@/components/ui';
import type { Role } from '@/types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  minRole?: Role;
  requireAuth?: boolean;
}

/**
 * 인증/권한 보호 라우트
 * @param minRole - 최소 필요 권한 (ASSOCIATE, MEMBER, OPERATOR, ADMIN), undefined면 로그인만 필요
 * @param requireAuth - 로그인 필수 여부 (기본 true)
 */
export default function ProtectedRoute({
  children,
  minRole = undefined,
  requireAuth = true,
}: ProtectedRouteProps) {
  const location = useLocation();
  const { isAuthenticated, isHydrated, user } = useAuth();
  const addToast = useUIStore((state) => state.addToast);
  const hasShownToast = useRef(false);

  const isPermissionDenied = minRole && !hasPermission(user?.role, minRole);

  // 권한 부족 시 토스트 표시 (렌더 중 state 변경 방지)
  useEffect(() => {
    if (!isHydrated || !isAuthenticated || !isPermissionDenied) return;
    if (hasShownToast.current) return;

    hasShownToast.current = true;
    const requiredRoleLabel = ROLE_LABELS[minRole!] ?? minRole;
    const currentRoleLabel = user?.role
      ? ROLE_LABELS[user.role] ?? user.role
      : '비회원';

    addToast({
      type: 'warning',
      title: '접근 권한 부족',
      message: `이 페이지는 ${requiredRoleLabel} 이상 권한이 필요합니다. (현재 권한: ${currentRoleLabel})`,
      duration: 5000,
    });
  }, [isHydrated, isAuthenticated, isPermissionDenied, minRole, user?.role, addToast]);

  // hydration 완료 전에는 로딩 스피너 표시
  if (!isHydrated) {
    return <FullPageSpinner />;
  }

  // 로그인 필요한데 안 되어있음
  if (requireAuth && !isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 권한 부족 → 홈으로 리다이렉트
  if (isPermissionDenied) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
