import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores';
import { authApi } from '@/api';

export const useAuth = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, setAuth, logout: clearAuth } = useAuthStore();

  const login = async (studentId, password) => {
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

  const logout = async () => {
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

  const recover = async (studentId, password) => {
    const { data } = await authApi.recover({ studentId, password });

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    setAuth(data.user, data.accessToken, data.refreshToken);

    return data;
  };

  return {
    user,
    isAuthenticated,
    login,
    logout,
    recover,
    isAssociate: user?.role === 'ASSOCIATE',
    isMember: user?.role === 'MEMBER',
    isOperator: user?.role === 'OPERATOR',
    isAdmin: user?.role === 'ADMIN',
    hasMinRole: (minRole) => {
      const roles = ['ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN'];
      const userRoleIndex = roles.indexOf(user?.role);
      const minRoleIndex = roles.indexOf(minRole);
      return userRoleIndex >= minRoleIndex;
    },
  };
};
