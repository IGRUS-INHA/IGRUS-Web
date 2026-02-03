import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores';
import { authApi } from '@/api';
import type { User } from '@/types/entities';
import type { Role } from '@/types/common';
import type { LoginResponse } from '@/types/api';
import { ROLES } from '@/types/common';

interface LoginResult extends LoginResponse {
  needsRecovery?: boolean;
}

interface UseAuthReturn {
  user: User | null;
  isAuthenticated: boolean;
  login: (studentId: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
  recover: (studentId: string, password: string) => Promise<LoginResponse>;
  isAssociate: boolean;
  isMember: boolean;
  isOperator: boolean;
  isAdmin: boolean;
  hasMinRole: (minRole: Role) => boolean;
}

const ROLE_ORDER: readonly Role[] = [
  ROLES.ASSOCIATE,
  ROLES.MEMBER,
  ROLES.OPERATOR,
  ROLES.ADMIN,
] as const;

export const useAuth = (): UseAuthReturn => {
  const navigate = useNavigate();
  const { user, isAuthenticated, setAuth, logout: clearAuth } = useAuthStore();

  const login = async (
    studentId: string,
    password: string
  ): Promise<LoginResult> => {
    const { data } = await authApi.login({ studentId, password });

    // 탈퇴 계정 복구 가능한 경우
    if (data.code === 'AUTH012' && data.recoverable) {
      return { needsRecovery: true, ...data };
    }

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    setAuth(data.user, data.accessToken, data.refreshToken);

    return data;
  };

  const logout = async (): Promise<void> => {
    try {
      await authApi.logout();
    } catch (error) {
      // 실패해도 로컬 정리 (토큰은 만료되므로 OK)
      console.error('Logout API error:', error);
    } finally {
      clearAuth();
      navigate('/login');
    }
  };

  const recover = async (
    studentId: string,
    password: string
  ): Promise<LoginResponse> => {
    const { data } = await authApi.recover({ studentId, password });

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    setAuth(data.user, data.accessToken, data.refreshToken);

    return data;
  };

  const hasMinRole = (minRole: Role): boolean => {
    if (!user?.role) return false;
    const userIndex = ROLE_ORDER.indexOf(user.role);
    const minIndex = ROLE_ORDER.indexOf(minRole);
    return userIndex >= minIndex;
  };

  return {
    user,
    isAuthenticated,
    login,
    logout,
    recover,
    isAssociate: user?.role === ROLES.ASSOCIATE,
    isMember: user?.role === ROLES.MEMBER,
    isOperator: user?.role === ROLES.OPERATOR,
    isAdmin: user?.role === ROLES.ADMIN,
    hasMinRole,
  };
};
