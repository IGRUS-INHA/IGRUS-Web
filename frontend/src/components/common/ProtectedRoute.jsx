import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores';
import { hasPermission } from '@/constants/permissions';

/**
 * 인증/권한 보호 라우트
 * @param {string|null} minRole - 최소 필요 권한 (ASSOCIATE, MEMBER, OPERATOR, ADMIN), null이면 로그인만 필요
 * @param {boolean} requireAuth - 로그인 필수 여부 (기본 true)
 */
export default function ProtectedRoute({
  children,
  minRole = null,
  requireAuth = true,
}) {
  const location = useLocation();
  const { isAuthenticated, user } = useAuthStore();

  // 로그인 필요한데 안 되어있음
  if (requireAuth && !isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 권한 부족 → 홈으로 리다이렉트
  if (minRole && !hasPermission(user?.role, minRole)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
